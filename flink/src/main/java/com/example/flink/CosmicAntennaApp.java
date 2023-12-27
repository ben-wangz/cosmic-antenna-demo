package com.example.flink;

import com.example.flink.data.*;
import com.example.flink.operation.*;
import com.example.flink.sink.JsonLogSink;
import com.example.flink.source.FPGASource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.FileUtils;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.flink.util.CoefficientDataUtil.*;

public class CosmicAntennaApp {
  private static final Logger LOGGER = LoggerFactory.getLogger(CosmicAntennaApp.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    Configuration configuration = CosmicAntennaConf.ConfigurationBuilder.build();
    Path tempDir = generateCoefficientDataFile(configuration.getInteger(CosmicAntennaConf.CHANNEL_SIZE));
    configuration.set(CosmicAntennaConf.COEFFICIENT_DATA_PATH, tempDir.toString());
    int timeSampleSize = configuration.getInteger(CosmicAntennaConf.TIME_SAMPLE_SIZE);
    int timeSampleUnitSize = configuration.getInteger(CosmicAntennaConf.TIME_SAMPLE_UNIT_SIZE);
    int beamFormingWindowSize =
        configuration.getInteger(CosmicAntennaConf.BEAM_FORMING_WINDOW_SIZE);
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    ExecutionConfig executionConfig = env.getConfig();
    executionConfig.setGlobalJobParameters(configuration);
    executionConfig.setAutoWatermarkInterval(20000L);
    GroupBeamOperator groupBeamOperator =
        GroupBeamOperator.builder()
            .algorithmNameList(
                IntStream.range(0, 3)
                    .mapToObj(index -> RandomStringUtils.randomAlphabetic(8))
                    .collect(Collectors.toList()))
            .build();
    SingleOutputStreamOperator<BeamData> beamDataStream =
        env.addSource(new FPGASource())
            .setParallelism(configuration.getInteger(CosmicAntennaConf.FPGA_SOURCE_PARALLELISM))
            .assignTimestampsAndWatermarks(
                WatermarkStrategy.<AntennaData>forBoundedOutOfOrderness(
                        // TODO configure duration
                        Duration.ofSeconds(timeSampleSize))
                    // TODO package counter should multiply with iteration count: redis counter++?
                    .withTimestampAssigner(
                        (antennaData, timestamp) ->
                            antennaData.getPackageCounter()
                                / (timeSampleSize / timeSampleUnitSize)))
            .flatMap(
                ChannelDataParser.builder()
                    .channelSize(configuration.getInteger(CosmicAntennaConf.CHANNEL_SIZE))
                    .timeSampleSize(configuration.getInteger(CosmicAntennaConf.TIME_SAMPLE_SIZE))
                    .build())
            .keyBy(ChannelAntennaData::getChannelId)
            .window(
                SlidingEventTimeWindows.of(
                    // TODO configure window size
                    Time.milliseconds(timeSampleSize), Time.milliseconds(timeSampleSize)))
            .aggregate(
                ChannelMerge.builder()
                    .timeSampleSize(timeSampleSize)
                    .antennaSize(configuration.getInteger(CosmicAntennaConf.ANTENNA_SIZE))
                    .build())
            .flatMap(
                ChannelDataUnitSplitter.builder()
                    .timeSampleSize(timeSampleSize)
                    .timeSampleUnitSize(timeSampleUnitSize)
                    .build())
            .keyBy((KeySelector<ChannelData, Integer>) ChannelData::getChannelId)
            .window(
                SlidingEventTimeWindows.of(
                    Time.milliseconds(timeSampleSize), Time.milliseconds(timeSampleSize)))
            .apply(
                BeamFormingWindowFunction.builder()
                    .channelSize(configuration.getInteger(CosmicAntennaConf.CHANNEL_SIZE))
                    .beamSize(configuration.getInteger(CosmicAntennaConf.BEAM_SIZE))
                    .antennaSize(configuration.getInteger(CosmicAntennaConf.ANTENNA_SIZE))
                    .timeSampleUnitSize(timeSampleUnitSize)
                    .beamFormingWindowSize(beamFormingWindowSize)
                    .coefficientDataList(retrieveCoefficientDataList(
                            configuration.getString(CosmicAntennaConf.COEFFICIENT_DATA_PATH))
                    )
                    .build())
            .keyBy((KeySelector<ChannelBeamData, Integer>) ChannelBeamData::getBeamId)
            .window(
                // TODO configure window size
                SlidingEventTimeWindows.of(
                    Time.milliseconds(timeSampleSize), Time.milliseconds(timeSampleSize)))
            .process(groupBeamOperator);
    for (OutputTag<BeamData> outputTag : groupBeamOperator.getOutputTagList()) {
      beamDataStream
          .getSideOutput(outputTag)
          .addSink(JsonLogSink.<BeamData>builder().build())
          .setParallelism(1)
          .name(outputTag.toString() + "_sink");
    }
    env.execute("transform example of sensor reading");
    FileUtils.deleteDirectoryQuietly(tempDir.toFile());
  }
}
