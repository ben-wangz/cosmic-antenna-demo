package com.example.flink.data;

import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class SensorReading implements Serializable {
    private Integer antennaId;
    private Integer channelId;
    private Long counter;
    private byte real;
    private byte imaginary;

    @Builder(toBuilder = true)
    @Jacksonized
    public SensorReading(
            Integer antennaId,
            Integer channelId,
            Long counter,
            byte real,
            byte imaginary) {
        this.antennaId = antennaId;
        this.channelId = channelId;
        this.counter = counter;
        this.real = real;
        this.imaginary = imaginary;
    }

    public Integer real() {
        return real & 0xff;
    }

    public Integer imaginary() {
        return imaginary & 0xff;
    }
}
