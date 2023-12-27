package com.example.flink.data;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ChannelBeamData {
  private Integer channelId;
  private Integer beamId;
  @ToString.Exclude private byte[] realArray;
  @ToString.Exclude private byte[] imaginaryArray;

  @Builder
  @Jacksonized
  public ChannelBeamData(
      @NonNull Integer channelId,
      @NonNull Integer beamId,
      @NonNull byte[] realArray,
      @NonNull byte[] imaginaryArray) {
    this.channelId = channelId;
    this.beamId = beamId;
    this.realArray = realArray;
    this.imaginaryArray = imaginaryArray;
  }
}
