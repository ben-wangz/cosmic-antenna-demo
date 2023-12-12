package com.example.flink;

import static org.bytedeco.opencv.global.opencv_core.multiply;

import java.time.Duration;
import java.util.Optional;

import com.example.flink.source.TempSource;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.opencv.opencv_core.Mat;

import com.example.flink.data.SampleData;
import com.example.flink.data.SensorReading;
import com.example.flink.sink.SensorSink;
import com.example.flink.source.SensorSource;
import com.google.common.base.Preconditions;

public class SensorApp {
	public static void main(String[] args) throws Exception {
		// read configuration from environment variables
		int timeSampleSize = Optional.ofNullable(System.getenv("TIME_SAMPLE_SIZE"))
				.map(envString -> Integer.parseInt(envString))
				.orElse(2048);
		int timeSampleUnitSize = Optional.ofNullable(System.getenv("TIME_SAMPLE_UNIT_SIZE"))
				.map(envString -> Integer.parseInt(envString))
				.orElse(64);
		Preconditions.checkArgument(
				0 == timeSampleSize % timeSampleUnitSize,
				"timeSampleSize (%s) should be divisible by timeSampleUnitSize(%s)",
				timeSampleSize, timeSampleUnitSize);
		int antennaSize = Optional.ofNullable(System.getenv("ANTENNA_SIZE"))
				.map(envString -> Integer.parseInt(envString))
				.orElse(224);
		long startCounter = Optional.ofNullable(System.getenv("START_COUNTER"))
				.map(envString -> Long.parseLong(envString))
				.orElse(0L);
		long sleepTimeInterval = Optional.ofNullable(System.getenv("SLEEP_TIME_INTERVAL"))
				.map(envString -> Long.parseLong(envString))
				.orElse(1000L);
		int serverPort = Optional.ofNullable(System.getenv("FPGA_SERVER_PORT"))
				.map(envString -> Integer.parseInt(envString))
				.orElse(18888);
		int UDPPackageSize = Optional.ofNullable(System.getenv("FPGA_PACKAGE_SIZE"))
				.map(envString -> Integer.parseInt(envString))
				.orElse(8192);
		// configure flink environment
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(
				new Configuration()
						.set(CosmicAntennaConf.TIME_SAMPLE_SIZE, timeSampleSize)
						.set(CosmicAntennaConf.TIME_SAMPLE_UNIT_SIZE, timeSampleUnitSize)
						.set(CosmicAntennaConf.ANTENNA_SIZE, antennaSize)
						.set(CosmicAntennaConf.START_COUNTER, startCounter)
						.set(CosmicAntennaConf.SLEEP_TIME_INTERVAL, sleepTimeInterval)
						.set(CosmicAntennaConf.FPGA_SERVER_PORT, serverPort)
						.set(CosmicAntennaConf.FPGA_PACKAGE_SIZE, UDPPackageSize)
		);
		// configure watermark interval
		env.getConfig().setAutoWatermarkInterval(1000L);
		DataStream<SampleData> sensorReadingStream = env
				.addSource(new TempSource())
				.assignTimestampsAndWatermarks(
						WatermarkStrategy
								.<SampleData>forBoundedOutOfOrderness(Duration.ofMillis(timeSampleUnitSize * 10))
								.withTimestampAssigner((sampleData, timestamp) -> sampleData.getStartCounter()));
		DataStream<SensorReading> transformedReadingStream = sensorReadingStream
				.flatMap(new FlatMapFunction<SampleData, SampleData>() {
					@Override
					public void flatMap(SampleData sampleData, Collector<SampleData> collector) throws Exception {
						int splitSize = timeSampleSize / timeSampleUnitSize;
						for (int index = 0; index < splitSize; index++) {
							byte[] realArray = new byte[timeSampleUnitSize];
							byte[] imaginaryArray = new byte[timeSampleUnitSize];
							int sourceArrayStartIndex = index * timeSampleUnitSize;
							System.arraycopy(sampleData.getRealArray(), sourceArrayStartIndex, realArray, 0,
									timeSampleUnitSize);
							System.arraycopy(sampleData.getImaginaryArray(), sourceArrayStartIndex, imaginaryArray, 0,
									timeSampleUnitSize);
							collector.collect(sampleData.toBuilder()
									.startCounter(sampleData.getStartCounter() + index * timeSampleUnitSize)
									.realArray(realArray)
									.imaginaryArray(imaginaryArray)
									.build());
						}
					}
				})
				.keyBy(new KeySelector<SampleData, Integer>() {
					@Override
					public Integer getKey(SampleData sampleData) throws Exception {
						return sampleData.getChannelId();
					}
				})
				.window(SlidingEventTimeWindows.of(
						Time.milliseconds(timeSampleUnitSize),
						Time.milliseconds(timeSampleUnitSize)))
				.process(new ProcessWindowFunction<SampleData, SampleData, Integer, TimeWindow>() {
					@Override
					public void open(Configuration configuration) throws Exception {
					};

					@Override
					public void process(Integer channelId, Context context, Iterable<SampleData> iterable,
							Collector<SampleData> collector) throws Exception {
						// all SampleData of all antenna will be received
						// if not, interpret the missing data with 0
						try (Mat realMat = new Mat(
								antennaSize,
								timeSampleUnitSize,
								org.bytedeco.opencv.global.opencv_core.CV_64FC1);
								Mat imaginaryMat = new Mat(
										antennaSize,
										timeSampleUnitSize,
										org.bytedeco.opencv.global.opencv_core.CV_64FC1)) {
							Indexer realMatIndexer = realMat.createIndexer();
							Indexer imaginaryMatIndexer = imaginaryMat.createIndexer();
							for (SampleData sampleData : iterable) {
								int antennaId = sampleData.getAntennaId();
								byte[] realArray = sampleData.getRealArray();
								byte[] imaginaryArray = sampleData.getImaginaryArray();
								for (int index = 0; index < realArray.length; index++) {
									long[] position = new long[] { antennaId, index };
									realMatIndexer.putDouble(position, realArray[index]);
									imaginaryMatIndexer.putDouble(position, imaginaryArray[index]);
								}
								// remove
								collector.collect(sampleData);
							}
							// TODO multiply(realMat, imaginaryMat, coefficientRealMat,
							// coefficientImaginaryMat);
						}
					}
				})
				.flatMap(new FlatMapFunction<SampleData, SensorReading>() {
					@Override
					public void flatMap(SampleData sampleData, Collector<SensorReading> collector)
							throws Exception {
						byte[] realArray = sampleData.getRealArray();
						byte[] imaginaryArray = sampleData.getImaginaryArray();
						Preconditions.checkArgument(
								realArray.length == imaginaryArray.length,
								"real array(%s) and imaginary array(%s) should have the same length",
								realArray.length,
								imaginaryArray.length);
						for (int index = 0; index < realArray.length; index++) {
							collector.collect(SensorReading.builder()
									.channelId(sampleData.getChannelId())
									.antennaId(sampleData.getAntennaId())
									.counter(sampleData.getStartCounter() + index)
									.real(realArray[index])
									.imaginary(imaginaryArray[index])
									.build());
						}
					}
				}).returns(Types.POJO(SensorReading.class));
		transformedReadingStream.addSink(SensorSink.builder().build());
		env.execute("transform example of sensor reading");
	}
}
