package com.example.fpga.handler;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleDataHandler extends SimpleChannelInboundHandler<Object> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataHandler.class);

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object msg) {
    DatagramPacket packet = (DatagramPacket) msg;
    byte[] bytes = ByteBufUtil.getBytes(packet.content());
    ByteBuffer antennaId = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, 1));
    ByteBuffer counter = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 1, 8));

    byte[] paddedCounter = new byte[8];
    System.arraycopy(counter.array(), 0, paddedCounter, 1, counter.array().length);

    LOGGER.info(
        "Response  antennaId:{}, counter:{} ",
        antennaId.get() & 0xFF,
        ByteBuffer.wrap(paddedCounter).getLong());
  }
}
