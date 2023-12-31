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
public class BeamData {
  private Integer beamId;
  @ToString.Exclude private byte[] realArray;
  @ToString.Exclude private byte[] imaginaryArray;

  @Builder
  @Jacksonized
  public BeamData(
      @NonNull Integer beamId, @NonNull byte[] realArray, @NonNull byte[] imaginaryArray) {
    this.beamId = beamId;
    this.realArray = realArray;
    this.imaginaryArray = imaginaryArray;
  }
}
