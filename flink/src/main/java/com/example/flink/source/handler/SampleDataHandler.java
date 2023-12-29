package com.example.flink.source.handler;

import com.example.flink.data.AntennaData;
import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import java.util.Arrays;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.SimpleChannelInboundHandler;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode(callSuper = true)
@ToString
public class SampleDataHandler extends SimpleChannelInboundHandler<byte[]> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataHandler.class);
  private final SourceFunction.SourceContext<AntennaData> sourceContext;
  private final int dataSize;

  @Builder
  @Jacksonized
  public SampleDataHandler(
      SourceFunction.SourceContext<AntennaData> sourceContext, Integer dataSize) {
    Preconditions.checkNotNull(sourceContext, "sourceContext is null");
    Preconditions.checkNotNull(dataSize, "dataChunkSize is null");
    this.sourceContext = sourceContext;
    this.dataSize = dataSize;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes)
      throws Exception {
    ByteBuffer antennaId = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, 1));
    ByteBuffer packageCounter = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 1, 8));
    byte[] paddedCounter = new byte[8];
    System.arraycopy(packageCounter.array(), 0, paddedCounter, 1, packageCounter.array().length);
    ByteBuffer realArray = ByteBuffer.wrap(new byte[dataSize / 2]);
    ByteBuffer imaginaryArray = ByteBuffer.wrap(new byte[dataSize / 2]);
    for (int i = 8; i < bytes.length; i++) {
      if (i % 2 == 0) {
        realArray.put(bytes[i]);
      } else {
        imaginaryArray.put(bytes[i]);
      }
    }
    AntennaData antennaData =
        AntennaData.builder()
            .antennaId(antennaId.get() & 0xFF)
            .packageCounter(ByteBuffer.wrap(paddedCounter).getLong())
            .realArray(realArray.array())
            .imaginaryArray(imaginaryArray.array())
            .build();
    LOGGER.debug(
        "server got an antenna data item {}, detail info[length:{}, header:{}]",
        antennaData,
        antennaData.getRealArray().length,
        Arrays.toString(Arrays.copyOfRange(antennaData.getRealArray(), 0, 10)));
    if (null != sourceContext) {
      sourceContext.collect(antennaData);
    }
  }
}
