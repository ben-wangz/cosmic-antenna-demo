package com.example.flink;

import static com.example.flink.util.CoefficientDataUtil.*;

import com.example.flink.data.*;
import com.example.flink.operation.*;
import com.example.flink.source.FPGASource;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.FileUtils;

public class CosmicAntennaApp {

  public static void main(String[] args) throws Exception {
    Configuration configuration = CosmicAntennaConf.ConfigurationBuilder.build();
    // TODO separate coefficient matrix generation from main function
    int channelSize = configuration.getInteger(CosmicAntennaConf.CHANNEL_SIZE);
    //    Path tempDir = generateCoefficientDataFile(channelSize);
    Path tempDir =
        Path.of(
            "C:\\Users\\Administrator\\AppData\\Local\\Temp\\cosmic-antenna4896456594933226445");
    configuration.set(CosmicAntennaConf.COEFFICIENT_DATA_PATH, tempDir.toString());
    int timeSampleSize = configuration.getInteger(CosmicAntennaConf.TIME_SAMPLE_SIZE);
    int timeSampleUnitSize = configuration.getInteger(CosmicAntennaConf.TIME_SAMPLE_UNIT_SIZE);
    int antennaSize = configuration.getInteger(CosmicAntennaConf.ANTENNA_SIZE);
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
    //    SingleOutputStreamOperator<BeamData> beamDataStream =
    env.addSource(new FPGASource())
        .setParallelism(configuration.getInteger(CosmicAntennaConf.FPGA_SOURCE_PARALLELISM))
        .assignTimestampsAndWatermarks(
            WatermarkStrategy.<AntennaData>forBoundedOutOfOrderness(
                    // TODO configure duration
                    Duration.ofSeconds(timeSampleSize))
                // TODO package counter should multiply with iteration count: redis counter++?
                .withTimestampAssigner(
                    (antennaData, timestamp) ->
                        antennaData.getPackageCounter() * (timeSampleSize / timeSampleUnitSize)))
        .flatMap(
            ChannelDataParser.builder()
                .channelSize(channelSize)
                .timeSampleSize(timeSampleSize)
                .timeSampleUnitSize(timeSampleUnitSize)
                .build())
        .keyBy(ChannelAntennaData::getChannelId)
        .window(TumblingEventTimeWindows.of(Time.milliseconds(timeSampleSize / timeSampleUnitSize)))
        .aggregate(
            ChannelMerge.builder().timeSampleSize(timeSampleSize).antennaSize(antennaSize).build())
        .print();
    //            .flatMap(
    //                ChannelDataUnitSplitter.builder()
    //                    .antennaSize(antennaSize)
    //                    .timeSampleSize(timeSampleSize)
    //                    .timeSampleUnitSize(timeSampleUnitSize)
    //                    .build())
    //            .keyBy((KeySelector<ChannelData, Integer>) ChannelData::getChannelId)
    //            .window(
    //                SlidingEventTimeWindows.of(
    //                    Time.milliseconds(timeSampleSize), Time.milliseconds(timeSampleSize)))
    //            .apply(
    //                BeamFormingWindowFunction.builder()
    //                    .channelSize(channelSize)
    //                    .beamSize(configuration.getInteger(CosmicAntennaConf.BEAM_SIZE))
    //                    .antennaSize(antennaSize)
    //                    .timeSampleUnitSize(timeSampleUnitSize)
    //
    // .beamFormingWindowSize(configuration.getInteger(CosmicAntennaConf.BEAM_FORMING_WINDOW_SIZE))
    //                    .coefficientDataList(
    //                        retrieveCoefficientDataList(
    //                            configuration.getString(CosmicAntennaConf.COEFFICIENT_DATA_PATH)))
    //                    .build())
    //            .keyBy((KeySelector<ChannelBeamData, Integer>) ChannelBeamData::getBeamId)
    //            .window(
    //                // TODO configure window size
    //                SlidingEventTimeWindows.of(
    //                    Time.milliseconds(timeSampleSize), Time.milliseconds(timeSampleSize)))
    //            .process(groupBeamOperator);
    //    for (OutputTag<BeamData> outputTag : groupBeamOperator.getOutputTagList()) {
    //      beamDataStream
    //          .getSideOutput(outputTag)
    //          .addSink(JsonLogSink.<BeamData>builder().build())
    //          .setParallelism(1)
    //          .name(outputTag.toString() + "_sink");
    //    }
    env.execute("transform example of sensor reading");
    FileUtils.deleteDirectoryQuietly(tempDir.toFile());
  }
}
