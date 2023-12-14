package com.example.flink.source.handler;

import lombok.Builder;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBufUtil;
import org.apache.flink.shaded.netty4.io.netty.buffer.Unpooled;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.DatagramPacket;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;


@Builder
public class MessageDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageDecoder.class);

    private int timeSampleSize;

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {

        ByteBuf in = packet.content();
        int readableBytes = in.readableBytes();
        if (readableBytes <= 0) {
            LOGGER.warn("Got empty UDP package");
            return;
        } else if (readableBytes != (12 + timeSampleSize * 2)) {
            LOGGER.error("Got an unacceptable UDP package");
            return;
        }

        byte[] bytes = ByteBufUtil.getBytes(in);
        out.add(bytes);

        ByteBuf buf = Unpooled.wrappedBuffer(Arrays.copyOfRange(bytes, 0, 12));
        ctx.channel().writeAndFlush(new DatagramPacket(buf, packet.sender()));
    }

}
