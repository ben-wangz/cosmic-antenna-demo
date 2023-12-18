package com.example.flink.data;

import com.google.common.base.Preconditions;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@EqualsAndHashCode
@ToString
public class CoefficientData {
    private final Integer channelId;
    private final byte[] realArray;
    private final byte[] imaginaryArray;

    @Builder
    @Jacksonized
    public CoefficientData(
            @NonNull Integer channelId,
            byte @NonNull [] realArray,
            byte @NonNull [] imaginaryArray) {
        Preconditions.checkArgument(
                realArray.length == imaginaryArray.length,
                "real array(%s) and imaginary array(%s) should have the same length",
                realArray.length, imaginaryArray.length);
        this.channelId = channelId;
        this.realArray = realArray;
        this.imaginaryArray = imaginaryArray;
    }
}
