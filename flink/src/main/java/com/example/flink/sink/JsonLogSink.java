package com.example.flink.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Builder
public class JsonLogSink<T> extends RichSinkFunction<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(JsonLogSink.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void invoke(T value, Context context) throws Exception {
    LOGGER.info(OBJECT_MAPPER.writeValueAsString(value));
  }
}
