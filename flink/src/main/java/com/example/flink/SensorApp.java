package com.example.flink;

import com.example.flink.data.BeamData;
import com.example.flink.data.CoefficientData;
import com.example.flink.data.SampleData;
import com.example.flink.data.SensorReading;
import com.example.flink.operation.GroupByBeam;
import com.example.flink.operation.MultiplyWithCoefficient;
import com.example.flink.sink.SensorSink;
import com.example.flink.source.ServerSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.FileUtils;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SensorApp {
        private static final Logger LOGGER = LoggerFactory.getLogger(SensorApp.class);

        public static void main(String[] args) throws Exception {
                // generate random coefficient matrix
                Path tempFilePath = generateCoefficientMatrix();
                // read configuration from environment variables
                int timeSampleSize = Optional.ofNullable(System.getenv("TIME_SAMPLE_SIZE"))
                                .map(Integer::parseInt)
                                .orElse(2048);
                int timeSampleUnitSize = Optional.ofNullable(System.getenv("TIME_SAMPLE_UNIT_SIZE"))
                                .map(Integer::parseInt)
                                .orElse(64);
                Preconditions.checkArgument(
                                0 == timeSampleSize % timeSampleUnitSize,
                                "timeSampleSize (%s) should be divisible by timeSampleUnitSize(%s)",
                                timeSampleSize, timeSampleUnitSize);
                int antennaSize = Optional.ofNullable(System.getenv("ANTENNA_SIZE"))
                                .map(Integer::parseInt)
                                .orElse(224);
                long startCounter = Optional.ofNullable(System.getenv("START_COUNTER"))
                                .map(Long::parseLong)
                                .orElse(0L);
                long sleepTimeInterval = Optional.ofNullable(System.getenv("SLEEP_TIME_INTERVAL"))
                                .map(Long::parseLong)
                                .orElse(1000L);
                int UDPPackageSize = Optional.ofNullable(System.getenv("FPGA_PACKAGE_SIZE"))
                                .map(Integer::parseInt)
                                .orElse(8192);
                // configure flink environment
                StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(
                                new Configuration()
                                                .set(CosmicAntennaConf.TIME_SAMPLE_SIZE, timeSampleSize)
                                                .set(CosmicAntennaConf.TIME_SAMPLE_UNIT_SIZE, timeSampleUnitSize)
                                                .set(CosmicAntennaConf.ANTENNA_SIZE, antennaSize)
                                                .set(CosmicAntennaConf.START_COUNTER, startCounter)
                                                .set(CosmicAntennaConf.SLEEP_TIME_INTERVAL, sleepTimeInterval)
                                                .set(CosmicAntennaConf.FPGA_PACKAGE_SIZE, UDPPackageSize)
                                                .set(CosmicAntennaConf.COEFFICIENT_DATA_PATH, tempFilePath.toString()));
                // configure watermark interval
                env.getConfig().setAutoWatermarkInterval(1000L);
                DataStream<SampleData> sensorReadingStream = env
                                .addSource(new ServerSource())
                                .setParallelism(1)
                                .assignTimestampsAndWatermarks(
                                                WatermarkStrategy
                                                                .<SampleData>forBoundedOutOfOrderness(Duration
                                                                                .ofMillis(timeSampleUnitSize * 10))
                                                                .withTimestampAssigner(
                                                                                (sampleData, timestamp) -> sampleData
                                                                                                .getStartCounter()));

                DataStream<SensorReading> transformedReadingStream = sensorReadingStream
                                .flatMap(new FlatMapFunction<SampleData, SampleData>() {
                                        @Override
                                        public void flatMap(SampleData sampleData, Collector<SampleData> collector)
                                                        throws Exception {
                                                int splitSize = timeSampleSize / timeSampleUnitSize;
                                                for (int index = 0; index < splitSize; index++) {
                                                        byte[] realArray = new byte[timeSampleUnitSize];
                                                        byte[] imaginaryArray = new byte[timeSampleUnitSize];
                                                        int sourceArrayStartIndex = index * timeSampleUnitSize;
                                                        System.arraycopy(sampleData.getRealArray(),
                                                                        sourceArrayStartIndex, realArray, 0,
                                                                        timeSampleUnitSize);
                                                        System.arraycopy(sampleData.getImaginaryArray(),
                                                                        sourceArrayStartIndex, imaginaryArray, 0,
                                                                        timeSampleUnitSize);
                                                        collector.collect(sampleData.toBuilder()
                                                                        .startCounter(sampleData.getStartCounter()
                                                                                        + index * timeSampleUnitSize)
                                                                        .realArray(realArray)
                                                                        .imaginaryArray(imaginaryArray)
                                                                        .build());
                                                }
                                        }
                                })
                                .keyBy((KeySelector<SampleData, Integer>) SampleData::getChannelId)
                                .window(SlidingEventTimeWindows.of(
                                                Time.milliseconds(timeSampleUnitSize),
                                                Time.milliseconds(timeSampleUnitSize)))
                                .process(new MultiplyWithCoefficient())
                                .keyBy((KeySelector<BeamData, Integer>) BeamData::getBeamId)
                                .window(SlidingEventTimeWindows.of(
                                                Time.milliseconds(timeSampleUnitSize),
                                                Time.milliseconds(timeSampleUnitSize)))
                                .process(new ProcessWindowFunction<BeamData, BeamData, Integer, TimeWindow>() {
                                        @Override
                                        public void process(Integer integer,
                                                        ProcessWindowFunction<BeamData, BeamData, Integer, TimeWindow>.Context context,
                                                        Iterable<BeamData> elements, Collector<BeamData> out)
                                                        throws Exception {
                                                for (BeamData beamData : elements) {
                                                        out.collect(beamData);
                                                }
                                        }
                                }).flatMap(new FlatMapFunction<BeamData, SensorReading>() {
                                        @Override
                                        public void flatMap(BeamData beamData, Collector<SensorReading> collector)
                                                        throws Exception {
                                                for (int index = 0; index < beamData.getResultArray().length; index++) {
                                                        collector.collect(SensorReading.builder()
                                                                        .channelId(beamData.getChannelId())
                                                                        .real(beamData.getResultArray()[index])
                                                                        .build());
                                                }
                                        }
                                }).returns(Types.POJO(SensorReading.class));
                transformedReadingStream.addSink(SensorSink.builder().build());
                env.execute("transform example of sensor reading");
                FileUtils.deleteFileOrDirectory(tempFilePath.toFile());
                LOGGER.info("deleted coefficient data temp file");
        }

        private static Path generateCoefficientMatrix() throws IOException {
                ObjectMapper OBJECT_MAPPER = new ObjectMapper();
                Path tempFile = Files.createTempFile("coefficient-", ".json");
                LOGGER.info("coefficient data saved in -> {}", tempFile.toString());
                Random random = new Random(666);

                List<CoefficientData> coefficientDataList = IntStream.range(0, 1000).boxed().map(channelId -> {
                        byte[] realArray = new byte[224 * 180];
                        byte[] imaginaryArray = new byte[224 * 180];
                        random.nextBytes(realArray);
                        random.nextBytes(imaginaryArray);
                        return CoefficientData.builder()
                                        .channelId(channelId)
                                        .realArray(realArray)
                                        .imaginaryArray(imaginaryArray)
                                        .build();
                }).collect(Collectors.toList());

                FileUtils.writeFileUtf8(tempFile.toFile(), OBJECT_MAPPER.writeValueAsString(coefficientDataList));
                return tempFile;
        }

}
