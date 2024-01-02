package com.example.flink.operation;

import com.example.flink.data.ChannelData;
import java.util.Arrays;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode
@ToString
public class ChannelDataUnitSplitter implements FlatMapFunction<ChannelData, ChannelData> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelDataUnitSplitter.class);
  private static final long serialVersionUID = 4373213454006503889L;
  private final int timeSampleSize;
  private final int timeSampleUnitSize;
  private final int antennaSize;

  @Builder
  @Jacksonized
  public ChannelDataUnitSplitter(int timeSampleSize, int timeSampleUnitSize, int antennaSize) {
    this.timeSampleSize = timeSampleSize;
    this.timeSampleUnitSize = timeSampleUnitSize;
    this.antennaSize = antennaSize;
  }

  @Override
  public void flatMap(ChannelData channelData, Collector<ChannelData> collector) throws Exception {
    LOGGER.debug(
        "before channel data unit split -> {}, data length {}, header:{}",
        channelData,
        channelData.getRealArray().length,
        Arrays.toString(channelData.getRealArray()));
    int unitValueSize = antennaSize * timeSampleUnitSize;
    for (int unitIndex = 0; unitIndex < timeSampleSize / timeSampleUnitSize; unitIndex++) {
      byte[] realArray = new byte[unitValueSize];
      byte[] imaginaryArray = new byte[unitValueSize];
      for (int antennaIndex = 0; antennaIndex < antennaSize; antennaIndex++) {
        int startIndexOfChannelData =
            antennaIndex * timeSampleSize + unitIndex * timeSampleUnitSize;
        int startIndexOfUnitChannelData = antennaIndex * timeSampleUnitSize;
        System.arraycopy(
            channelData.getRealArray(),
            startIndexOfChannelData,
            realArray,
            startIndexOfUnitChannelData,
            timeSampleUnitSize);
        System.arraycopy(
            channelData.getImaginaryArray(),
            startIndexOfChannelData,
            imaginaryArray,
            startIndexOfUnitChannelData,
            timeSampleUnitSize);
      }
      LOGGER.debug("after channel data unit split, data length change to {}", realArray.length);
      collector.collect(
          channelData.toBuilder()
              .counter(channelData.getCounter() + unitIndex)
              .realArray(realArray)
              .imaginaryArray(imaginaryArray)
              .build());
    }
  }
}
