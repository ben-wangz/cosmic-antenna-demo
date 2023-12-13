package com.example.flink.source.handler;

import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBufUtil;
import org.apache.flink.shaded.netty4.io.netty.buffer.Unpooled;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelFutureListener;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.DatagramPacket;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * {@link MessageDecoder} is the Message decoder from byte to string using {@link MessageToMessageDecoder}.
 *
 * @author Sameer Narkhede See <a href="https://narkhedesam.com">https://narkhedesam.com</a>
 * @since Sept 2020
 */
public class MessageDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws Exception {

        ByteBuf in = packet.content();
        int readableBytes = in.readableBytes();
        if (readableBytes <= 0) {
            LOGGER.warn("[MessageDecoder] got empty packet");
            return;
        }

        out.add(ByteBufUtil.getBytes(in));

        final WriteListener listener = success -> {
            if (success) {
                LOGGER.debug("[MessageDecoder] response client success.");
            }else {
                LOGGER.warn("[MessageDecoder] response client failed.");
            }
        };

        ByteBuf buf = Unpooled.wrappedBuffer("[From Server] Message received.".getBytes());
        ctx.channel().writeAndFlush(new DatagramPacket(buf, packet.sender()))
                .addListener((ChannelFutureListener) future -> listener.messageRespond(future.isSuccess()));
    }

    public interface WriteListener {
        void messageRespond(boolean success);
    }

}
