package com.example.flink.operation;

import com.example.flink.data.ChannelAntennaData;
import com.example.flink.data.ChannelData;
import com.example.flink.data.ChannelDataACC;
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

@EqualsAndHashCode
@ToString
public class ChannelMerge
    implements AggregateFunction<ChannelAntennaData, ChannelDataACC, ChannelData> {
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
    Integer channelId = channelAntennaData.getChannelId();
    Long counter = channelAntennaData.getCounter();
    Integer accumulatorChannelId = channelDataFromAcc.getChannelId();
    Long accumulatorCounter = channelDataFromAcc.getCounter();
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
    return accumulator;
  }

  @Override
  public ChannelData getResult(ChannelDataACC accumulator) {
    return accumulator.getChannelData();
  }

  @Override
  public ChannelDataACC merge(ChannelDataACC accLeft, ChannelDataACC accRight) {
    ChannelData channelDataFromAccLeft = accLeft.getChannelData();
    ChannelData channelDataFromAccRight = accRight.getChannelData();
    Integer channelIdLeft = channelDataFromAccLeft.getChannelId();
    Long counterLeft = channelDataFromAccLeft.getCounter();
    Integer channelIdRight = channelDataFromAccRight.getChannelId();
    Long counterRight = channelDataFromAccRight.getCounter();
    if (-1 != channelIdLeft && -1 != channelIdRight) {
      Preconditions.checkArgument(
          channelIdLeft.equals(channelIdRight),
          "channelIdLeft(%s) != channelIdRight(%s)",
          channelIdLeft,
          channelIdRight);
    }
    if (-1L != counterLeft && -1L != counterRight) {
      Preconditions.checkArgument(
          counterLeft.equals(counterRight),
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
    return ChannelDataACC.builder()
        .gatheredAntennaIdSet(
            Stream.of(accLeft.getGatheredAntennaIdSet(), accRight.getGatheredAntennaIdSet())
                .flatMap(Set::stream)
                .collect(Collectors.toSet()))
        .channelData(channelDataMerged)
        .build();
  }
}