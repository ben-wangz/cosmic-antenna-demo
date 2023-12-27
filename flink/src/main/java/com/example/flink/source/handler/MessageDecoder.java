package com.example.flink.source.handler;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBufUtil;
import org.apache.flink.shaded.netty4.io.netty.buffer.Unpooled;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.DatagramPacket;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode(callSuper = true)
@ToString
public class MessageDecoder extends MessageToMessageDecoder<DatagramPacket> {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageDecoder.class);
  private final int headerSize;
  private final int dataSize;

  @Builder
  @Jacksonized
  public MessageDecoder(Integer headerSize, Integer dataSize) {
    Preconditions.checkNotNull(headerSize, "headerSize is null");
    Preconditions.checkNotNull(dataSize, "dataSize is null");
    this.headerSize = headerSize;
    this.dataSize = dataSize;
  }

  @Override
  protected void decode(
      ChannelHandlerContext context, DatagramPacket datagramPacket, List<Object> out)
      throws Exception {
    ByteBuf input = datagramPacket.content();
    int packageSize = input.readableBytes();
    if (packageSize <= 0) {
      LOGGER.warn("Got empty UDP package");
      return;
    }
    if ((headerSize + dataSize * 2) != packageSize) {
      LOGGER.warn(
          "omit package: packageSize(%s) != (headerSize(%s) + dataSize(%s) * 2)",
          packageSize, headerSize, dataSize);
      return;
    }
    byte[] bytes = ByteBufUtil.getBytes(input);
    out.add(bytes);
    ByteBuf header = Unpooled.wrappedBuffer(Arrays.copyOfRange(bytes, 0, headerSize));
    context.channel().writeAndFlush(new DatagramPacket(header, datagramPacket.sender()));
  }
}
