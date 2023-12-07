package com.example.flink.data;

import com.google.common.base.Preconditions;

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
public class SampleData {
    private Integer channelId;
    private Integer antennaId;
    private Long startCounter;
    private byte[] realArray;
    private byte[] imaginaryArray;

    @Jacksonized
    @Builder(toBuilder = true)
    public SampleData(
            @NonNull Integer channelId,
            @NonNull Integer antennaId,
            @NonNull Long startCounter,
            @NonNull byte[] realArray,
            @NonNull byte[] imaginaryArray) {
        this.channelId = channelId;
        this.antennaId = antennaId;
        this.startCounter = startCounter;
        this.realArray = realArray;
        this.imaginaryArray = imaginaryArray;
    }

    public int size() {
        Preconditions.checkArgument(
                realArray.length == imaginaryArray.length,
                "real array(%s) and imaginary array(%s) should have the same length",
                realArray.length, imaginaryArray.length);
        return realArray.length;
    }
}
