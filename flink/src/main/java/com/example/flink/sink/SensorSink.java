package com.example.flink.sink;

import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.flink.data.SensorReading;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;

@Builder
public class SensorSink extends RichSinkFunction<SensorReading>{
    private static final Logger LOGGER = LoggerFactory.getLogger(SensorReading.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void invoke(SensorReading value, Context context) throws Exception {
        LOGGER.info("sensor reading: {}", OBJECT_MAPPER.writeValueAsString(value));
    }
}
