package com.example.flink.data;

import com.google.common.base.Preconditions;
import java.io.Serializable;
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
public class AntennaData implements Serializable {
  private static final long serialVersionUID = 2791144471356852216L;
  private Integer antennaId;
  private Long packageCounter;
  @ToString.Exclude private byte[] realArray;
  @ToString.Exclude private byte[] imaginaryArray;

  @Jacksonized
  @Builder(toBuilder = true)
  public AntennaData(
      @NonNull Integer antennaId,
      @NonNull Long packageCounter,
      @NonNull byte[] realArray,
      @NonNull byte[] imaginaryArray) {
    this.antennaId = antennaId;
    this.packageCounter = packageCounter;
    this.realArray = realArray;
    this.imaginaryArray = imaginaryArray;
  }

  public int size() {
    Preconditions.checkArgument(
        realArray.length == imaginaryArray.length,
        "real array(%s) and imaginary array(%s) should have the same length",
        realArray.length,
        imaginaryArray.length);
    return realArray.length;
  }
}
