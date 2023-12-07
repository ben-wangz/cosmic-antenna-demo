package com.example.flink.data;

import com.google.common.base.Preconditions;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@EqualsAndHashCode
@ToString
public class CoefficientData {
    private Integer channelId;
    private Integer antennaLength;
    private Integer beamLength;
    private byte[] realArray;
    private byte[] imaginaryArray;

    @Builder
    @Jacksonized
    public CoefficientData(
            Integer channelId,
            Integer antennaLength,
            Integer beamLength,
            byte[] realArray,
            byte[] imaginaryArray) {
        int realArrayLength = realArray.length;
        int imaginaryArrayLength = imaginaryArray.length;
        Preconditions.checkArgument(
                realArrayLength == imaginaryArrayLength,
                "real array(%s) and imaginary array(%s) should have the same length",
                realArrayLength, imaginaryArrayLength);
        Preconditions.checkArgument(
                antennaLength * beamLength == realArrayLength,
                "antenna length(%s) * beam length(%s) should equal to real array length(%s)",
                antennaLength, beamLength, realArrayLength);
        this.channelId = channelId;
        this.antennaLength = antennaLength;
        this.beamLength = beamLength;
        this.realArray = realArray;
        this.imaginaryArray = imaginaryArray;
    }
}
