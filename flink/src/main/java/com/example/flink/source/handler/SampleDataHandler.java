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

@EqualsAndHashCode(callSuper = true)
@ToString
public class SampleDataHandler extends SimpleChannelInboundHandler<byte[]> {
  private SourceFunction.SourceContext<AntennaData> sourceContext;
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
    // TODO transform counter to long with utils
    byte[] paddedCounter = new byte[8];
    System.arraycopy(packageCounter.array(), 0, paddedCounter, 0, packageCounter.array().length);
    ByteBuffer realArray = ByteBuffer.wrap(new byte[dataSize]);
    ByteBuffer imaginaryArray = ByteBuffer.wrap(new byte[dataSize]);
    for (int i = 12; i < bytes.length; i++) {
      if (i % 2 == 0) {
        realArray.put(bytes[i]);
      } else {
        imaginaryArray.put(bytes[i]);
      }
    }
    AntennaData sampleData =
        AntennaData.builder()
            .antennaId(antennaId.get() & 0xFF)
            .packageCounter(ByteBuffer.wrap(paddedCounter).getLong())
            .realArray(realArray.array())
            .imaginaryArray(imaginaryArray.array())
            .build();
    if (null != sourceContext) {
      sourceContext.collect(sampleData);
    }
  }
}
