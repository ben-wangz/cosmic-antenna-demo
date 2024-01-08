package com.example.flink;

import com.example.flink.data.*;
import com.example.flink.operation.*;
import com.example.flink.sink.JsonLogSink;
import com.example.flink.source.FPGASource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
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
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.FileUtils;
import org.apache.flink.util.OutputTag;

public class CosmicAntennaApp {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    Configuration configuration = CosmicAntennaConf.ConfigurationBuilder.build();
    int channelSize = configuration.getInteger(CosmicAntennaConf.CHANNEL_SIZE);
    int timeSampleSize = configuration.getInteger(CosmicAntennaConf.TIME_SAMPLE_SIZE);
    int timeSampleUnitSize = configuration.getInteger(CosmicAntennaConf.TIME_SAMPLE_UNIT_SIZE);
    int beamFormingWindowSize =
        configuration.getInteger(CosmicAntennaConf.BEAM_FORMING_WINDOW_SIZE);
    int antennaSize = configuration.getInteger(CosmicAntennaConf.ANTENNA_SIZE);
    int businessRelatedWindowSize = timeSampleUnitSize * beamFormingWindowSize;
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    ExecutionConfig executionConfig = env.getConfig();
    executionConfig.setGlobalJobParameters(configuration);
    executionConfig.setAutoWatermarkInterval(1000L);
    GroupBeamOperator groupBeamOperator =
        GroupBeamOperator.builder()
            .channelSize(channelSize)
            .timeSampleSize(businessRelatedWindowSize)
            .algorithmNameList(
                IntStream.range(0, 3)
                    .mapToObj(index -> RandomStringUtils.randomAlphabetic(8))
                    .collect(Collectors.toList()))
            .build();
    SingleOutputStreamOperator<BeamData> beamDataStream =
        env.addSource(new FPGASource())
            .setParallelism(configuration.getInteger(CosmicAntennaConf.FPGA_SOURCE_PARALLELISM))
            .assignTimestampsAndWatermarks(
                WatermarkStrategy.<AntennaData>forBoundedOutOfOrderness(Duration.ofSeconds(1L))
                    // TODO package counter should multiply with iteration count: redis counter++
                    .withTimestampAssigner(
                        (antennaData, timestamp) ->
                            antennaData.getPackageCounter()
                                * (timeSampleSize / timeSampleUnitSize)))
            .flatMap(
                ChannelDataParser.builder()
                    .channelSize(channelSize)
                    .timeSampleSize(timeSampleSize)
                    .timeSampleUnitSize(timeSampleUnitSize)
                    .build())
            .keyBy((KeySelector<ChannelAntennaData, Integer>) ChannelAntennaData::getChannelId)
            .window(
                TumblingEventTimeWindows.of(Time.milliseconds(timeSampleSize / timeSampleUnitSize)))
            .aggregate(
                ChannelMerge.builder()
                    .timeSampleSize(timeSampleSize)
                    .antennaSize(antennaSize)
                    .build())
            .flatMap(
                ChannelDataUnitSplitter.builder()
                    .antennaSize(antennaSize)
                    .timeSampleSize(timeSampleSize)
                    .timeSampleUnitSize(timeSampleUnitSize)
                    .build())
            .keyBy((KeySelector<ChannelData, Integer>) ChannelData::getChannelId)
            .window(
                SlidingEventTimeWindows.of(
                    Time.milliseconds(businessRelatedWindowSize),
                    Time.milliseconds(businessRelatedWindowSize)))
            .apply(
                BeamFormingWindowFunction.builder()
                    .channelSize(channelSize)
                    .beamSize(configuration.getInteger(CosmicAntennaConf.BEAM_SIZE))
                    .antennaSize(antennaSize)
                    .timeSampleUnitSize(timeSampleUnitSize)
                    .beamFormingWindowSize(beamFormingWindowSize)
                    .coefficientDataList(
                        retrieveCoefficientDataList(
                            configuration.getString(CosmicAntennaConf.COEFFICIENT_DATA_PATH)))
                    .build())
            .keyBy((KeySelector<ChannelBeamData, Integer>) ChannelBeamData::getBeamId)
            .window(
                SlidingEventTimeWindows.of(
                    Time.milliseconds(businessRelatedWindowSize),
                    Time.milliseconds(businessRelatedWindowSize)))
            .process(groupBeamOperator);
    for (OutputTag<BeamData> outputTag : groupBeamOperator.getOutputTagList()) {
      beamDataStream
          .getSideOutput(outputTag)
          .addSink(JsonLogSink.<BeamData>builder().build())
          .setParallelism(1)
          .name(outputTag.toString() + "_sink");
    }
    env.execute("transform example of sensor reading");
  }

  public static List<CoefficientData> retrieveCoefficientDataList(String dirPath)
      throws IOException {
    Collection<Path> dataFiles =
        FileUtils.listFilesInDirectory(
            Path.of(dirPath),
            path -> path.getFileName().toString().startsWith(CoefficientDataGenerator.FILE_PREFIX));
    return dataFiles.stream()
        .map(
            file -> {
              try {
                return OBJECT_MAPPER.readValue(
                    file.toFile(), new TypeReference<CoefficientData>() {});
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .collect(Collectors.toList());
  }
}
