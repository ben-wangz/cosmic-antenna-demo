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
  private final int timeSampleSize;
  private final int channelSize;

  @Builder
  @Jacksonized
  public ChannelDataParser(@NonNull Integer timeSampleSize, @NonNull Integer channelSize) {
    this.timeSampleSize = timeSampleSize;
    this.channelSize = channelSize;
  }

  @Override
  public void flatMap(AntennaData antennaData, Collector<ChannelAntennaData> collector)
      throws Exception {
    LOGGER.debug("before channel data parse, the real array length is {}", antennaData.getRealArray().length);
    for (int channelId = 0; channelId < channelSize; channelId++) {
      int startIndex = channelId * timeSampleSize;
      int endIndex = startIndex + timeSampleSize;
      LOGGER.debug("parse channel data -> channelId {}, real [{},{})", channelId, startIndex, endIndex);
      collector.collect(
          ChannelAntennaData.builder()
              .channelId(channelId)
              .antennaId(antennaData.getAntennaId())
              .counter(antennaData.getPackageCounter())
              .realArray(Arrays.copyOfRange(antennaData.getRealArray(), startIndex, endIndex))
              .imaginaryArray(
                  Arrays.copyOfRange(antennaData.getImaginaryArray(), startIndex, endIndex))
              .build());
    }
  }
}
