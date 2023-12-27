package com.example.flink.operation;

import com.example.flink.data.ChannelData;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

@EqualsAndHashCode
@ToString
public class ChannelDataUnitSplitter implements FlatMapFunction<ChannelData, ChannelData> {
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
      collector.collect(
          channelData.toBuilder()
              .counter(channelData.getCounter() + unitIndex)
              .realArray(realArray)
              .imaginaryArray(imaginaryArray)
              .build());
    }
  }
}
