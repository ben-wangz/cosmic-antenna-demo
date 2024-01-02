package com.example.flink.operation;

import com.example.flink.data.BeamData;
import com.example.flink.data.ChannelBeamData;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class GroupBeamOperator
    extends ProcessWindowFunction<ChannelBeamData, BeamData, Integer, TimeWindow> {
  private static final long serialVersionUID = -7828810423206304103L;
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupBeamOperator.class);
  private final List<OutputTag<BeamData>> outputTagList;
  private final Integer channelSize;
  private final Integer timeSampleSize;

  @Builder
  @Jacksonized
  public GroupBeamOperator(
      Integer channelSize, Integer timeSampleSize, List<String> algorithmNameList) {
    this.channelSize = channelSize;
    this.timeSampleSize = timeSampleSize;
    outputTagList =
        algorithmNameList.stream()
            .map(
                algorithmName ->
                    new OutputTag<BeamData>(algorithmName) {
                      private static final long serialVersionUID = 1745268980889408883L;
                    })
            .collect(Collectors.toList());
  }

  @Override
  public void process(
      Integer integer,
      ProcessWindowFunction<ChannelBeamData, BeamData, Integer, TimeWindow>.Context context,
      Iterable<ChannelBeamData> elements,
      Collector<BeamData> collector)
      throws Exception {
    List<ChannelBeamData> channelBeamDataList =
        StreamSupport.stream(elements.spliterator(), false).collect(Collectors.toList());
    LOGGER.info(
        "group beam operator got {} items, and it contains {} and array length is {}",
        channelBeamDataList.size(),
        channelBeamDataList.get(0),
        channelBeamDataList.get(0).getRealArray().length);

    int length = channelSize * timeSampleSize;
    byte[] realArray = new byte[length];
    byte[] imaginaryArray = new byte[length];
    channelBeamDataList.forEach(
        channelBeamData -> {
          Integer channelId = channelBeamData.getChannelId();
          System.arraycopy(
              channelBeamData.getRealArray(),
              0,
              realArray,
              channelId * timeSampleSize,
              timeSampleSize);
          System.arraycopy(
              channelBeamData.getImaginaryArray(),
              0,
              imaginaryArray,
              channelId * timeSampleSize,
              timeSampleSize);
        });
    BeamData beamData =
        BeamData.builder()
            .beamId(channelBeamDataList.get(0).getBeamId())
            .realArray(realArray)
            .imaginaryArray(imaginaryArray)
            .build();
    for (OutputTag<BeamData> outputTag : outputTagList) {
      context.output(outputTag, beamData);
    }
  }
}
