package com.example.flink.operation;

import com.example.flink.data.AntennaData;
import com.example.flink.data.ChannelAntennaData;
import java.util.Arrays;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode
@ToString
public class ChannelDataParser implements FlatMapFunction<AntennaData, ChannelAntennaData> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelDataParser.class);
  private static final long serialVersionUID = 89385729028727556L;
  private final int timeSampleUnitSize;
  private final int timeSampleSize;
  private final int channelSize;

  @Builder
  @Jacksonized
  public ChannelDataParser(
      @NonNull Integer timeSampleUnitSize,
      @NonNull Integer timeSampleSize,
      @NonNull Integer channelSize) {
    this.timeSampleUnitSize = timeSampleUnitSize;
    this.timeSampleSize = timeSampleSize;
    this.channelSize = channelSize;
  }

  @Override
  public void flatMap(AntennaData antennaData, Collector<ChannelAntennaData> collector)
      throws Exception {
    LOGGER.debug(
        "before channel data parse, original info [length:{}, header:{}]",
        antennaData.getRealArray().length,
        Arrays.toString(Arrays.copyOfRange(antennaData.getRealArray(), 0, 10)));
    for (int channelId = 0; channelId < channelSize; channelId++) {
      int startIndex = channelId * timeSampleSize;
      int endIndex = startIndex + timeSampleSize;
      LOGGER.debug(
          "during parse channel data -> channelId {}, extract index: [{},{})",
          channelId,
          startIndex,
          endIndex);
      ChannelAntennaData channelAntennaData =
          ChannelAntennaData.builder()
              .channelId(channelId)
              .antennaId(antennaData.getAntennaId())
              .counter(antennaData.getPackageCounter() * (timeSampleSize / timeSampleUnitSize))
              .realArray(Arrays.copyOfRange(antennaData.getRealArray(), startIndex, endIndex))
              .imaginaryArray(
                  Arrays.copyOfRange(antennaData.getImaginaryArray(), startIndex, endIndex))
              .build();
      LOGGER.debug(
          "after channel data  parse, got object -> {}, array info[length:{}, header:{}]",
          channelAntennaData,
          channelAntennaData.getRealArray().length,
          Arrays.toString(Arrays.copyOfRange(channelAntennaData.getRealArray(), 0, 10)));
      collector.collect(channelAntennaData);
    }
  }
}
