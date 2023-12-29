package com.example.flink.operation;

import com.example.flink.data.ChannelAntennaData;
import com.example.flink.data.ChannelData;
import com.example.flink.data.ChannelDataACC;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode
@ToString
public class ChannelMerge
    implements AggregateFunction<ChannelAntennaData, ChannelDataACC, ChannelData> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChannelMerge.class);
  private static final long serialVersionUID = -4045304070063939211L;
  private final Integer timeSampleSize;
  private final Integer antennaSize;

  @Builder
  @Jacksonized
  public ChannelMerge(Integer timeSampleSize, Integer antennaSize) {
    this.timeSampleSize = timeSampleSize;
    this.antennaSize = antennaSize;
  }

  @Override
  public ChannelDataACC createAccumulator() {
    int length = timeSampleSize * antennaSize;
    return ChannelDataACC.builder()
        .gatheredAntennaIdSet(new HashSet<>())
        .channelData(
            ChannelData.builder()
                .channelId(-1)
                .counter(-1L)
                .realArray(new byte[length])
                .imaginaryArray(new byte[length])
                .build())
        .build();
  }

  @Override
  public ChannelDataACC add(ChannelAntennaData channelAntennaData, ChannelDataACC accumulator) {
    ChannelData channelDataFromAcc = accumulator.getChannelData();
    accumulator.getGatheredAntennaIdSet().add(channelAntennaData.getAntennaId());
    Integer channelId = channelAntennaData.getChannelId();
    Long counter = channelAntennaData.getCounter();
    Integer accumulatorChannelId = channelDataFromAcc.getChannelId();
    Long accumulatorCounter = channelDataFromAcc.getCounter();
    LOGGER.info(
        "going to add two channel antenna data -> {}[length:{} header:{}] and acc -> {}[length:{}"
            + " header:{}]",
        channelAntennaData,
        channelAntennaData.getRealArray().length,
        Arrays.toString(Arrays.copyOfRange(channelAntennaData.getRealArray(), 0, 10)),
        accumulator,
        accumulator.getChannelData().getRealArray().length,
        Arrays.toString(Arrays.copyOfRange(accumulator.getChannelData().getRealArray(), 0, 10)));
    if (-1 == accumulatorChannelId) {
      channelDataFromAcc.setChannelId(channelId);
    } else {
      Preconditions.checkArgument(
          Objects.equals(channelId, accumulatorChannelId),
          "channelId(%s) != accumulatorChannelId(%s)",
          channelId,
          accumulatorChannelId);
    }
    if (-1 == accumulatorCounter) {
      channelDataFromAcc.setCounter(counter);
    } else {
      Preconditions.checkArgument(
          Objects.equals(counter, accumulatorCounter),
          "counter(%s) != accumulatorCounter(%s)",
          counter,
          accumulatorCounter);
    }
    int startIndex = channelAntennaData.getAntennaId() * timeSampleSize;
    System.arraycopy(
        channelAntennaData.getRealArray(),
        0,
        channelDataFromAcc.getRealArray(),
        startIndex,
        timeSampleSize);
    System.arraycopy(
        channelAntennaData.getImaginaryArray(),
        0,
        channelDataFromAcc.getImaginaryArray(),
        startIndex,
        timeSampleSize);
    LOGGER.info(
        "after adding return acc -> {}[length:{}, all:{}]",
        accumulator,
        accumulator.getChannelData().getRealArray().length,
        Arrays.toString(accumulator.getChannelData().getRealArray()));
    return accumulator;
  }

  @Override
  public ChannelData getResult(ChannelDataACC accumulator) {
    return accumulator.getChannelData();
  }

  @Override
  public ChannelDataACC merge(ChannelDataACC accLeft, ChannelDataACC accRight) {
    LOGGER.info("acc merge ... ");
    ChannelData channelDataFromAccLeft = accLeft.getChannelData();
    ChannelData channelDataFromAccRight = accRight.getChannelData();
    Integer channelIdLeft = channelDataFromAccLeft.getChannelId();
    Long counterLeft = channelDataFromAccLeft.getCounter();
    Integer channelIdRight = channelDataFromAccRight.getChannelId();
    Long counterRight = channelDataFromAccRight.getCounter();
    if (-1 != channelIdLeft && -1 != channelIdRight) {
      Preconditions.checkArgument(
          Objects.equals(channelIdLeft, channelIdRight),
          "channelIdLeft(%s) != channelIdRight(%s)",
          channelIdLeft,
          channelIdRight);
    }
    if (-1L != counterLeft && -1L != counterRight) {
      Preconditions.checkArgument(
          Objects.equals(counterLeft, counterRight),
          "counterLeft(%s) != counterRight(%s)",
          counterLeft,
          counterRight);
    }
    ChannelData channelDataMerged = channelDataFromAccLeft.toBuilder().build();
    for (Integer gatheredAntennaId : accRight.getGatheredAntennaIdSet()) {
      int startIndex = gatheredAntennaId * timeSampleSize;
      System.arraycopy(
          channelDataFromAccRight.getRealArray(),
          startIndex,
          channelDataMerged.getRealArray(),
          startIndex,
          timeSampleSize);
      System.arraycopy(
          channelDataFromAccRight.getImaginaryArray(),
          startIndex,
          channelDataMerged.getImaginaryArray(),
          startIndex,
          timeSampleSize);
    }
    LOGGER.info(
        "after channel merge, data array length is {}", channelDataMerged.getRealArray().length);
    return ChannelDataACC.builder()
        .gatheredAntennaIdSet(
            Stream.of(accLeft.getGatheredAntennaIdSet(), accRight.getGatheredAntennaIdSet())
                .flatMap(Set::stream)
                .collect(Collectors.toSet()))
        .channelData(channelDataMerged)
        .build();
  }
}
