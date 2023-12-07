package com.example.flink;

import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.example.flink.data.SensorReading;

public class SensorReadingTest {
    private static final Random RANDOM = new Random();

    @Test
    public void testReal() {
        int real = RANDOM.nextInt(256);
        int imaginary = RANDOM.nextInt(256);
        SensorReading sensorReading = SensorReading.builder()
                .antennaId(RANDOM.nextInt(10))
                .channelId(RANDOM.nextInt(10))
                .counter(RANDOM.nextLong())
                .real((byte) real)
                .imaginary((byte) imaginary)
                .build();
        Assertions.assertEquals(real, sensorReading.real());
        Assertions.assertEquals(imaginary, sensorReading.imaginary());
    }
}
