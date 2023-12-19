package com.example.fpga.handler;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

public class SampleDataHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataHandler.class);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        DatagramPacket packet = (DatagramPacket) msg;
        byte[] bytes = ByteBufUtil.getBytes(packet.content());
        ByteBuffer channelId = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, 2));
        ByteBuffer antennaId = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 2, 4));
        ByteBuffer counter = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 4, 12));
        LOGGER.info("Response channelId:{}, antennaId:{}, counter:{} ",
                channelId.getShort(), antennaId.getShort(), counter.getLong());
    }
}
