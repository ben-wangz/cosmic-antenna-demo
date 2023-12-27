package com.example.flink.data;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@ToString
public class CoefficientData implements Serializable {
  private static final long serialVersionUID = 7241510190414198351L;
  private final byte[] realArray;
  private final byte[] imaginaryArray;

  @Builder
  @Jacksonized
  public CoefficientData(
      @NonNull Integer channelId, @NonNull byte[] realArray, @NonNull byte[] imaginaryArray) {
    Preconditions.checkArgument(
        realArray.length == imaginaryArray.length,
        "real array(%s) and imaginary array(%s) should have the same length",
        realArray.length,
        imaginaryArray.length);
    this.realArray = realArray;
    this.imaginaryArray = imaginaryArray;
  }
}
