package com.example.flink.source.handler;

import com.example.flink.data.SampleData;
import com.example.flink.source.server.MockServer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.apache.flink.shaded.netty4.io.netty.buffer.Unpooled;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelFutureListener;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.SimpleChannelInboundHandler;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.DatagramPacket;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Builder
@AllArgsConstructor
public class SampleDataHandler extends SimpleChannelInboundHandler<SampleData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockServer.class);

    private SourceFunction.SourceContext<SampleData> sourceContext;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        LOGGER.error(cause.getLocalizedMessage());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SampleData sampleData) throws Exception {
        LOGGER.warn("[SampleDataHandler] Message Received : " + sampleData);
        if (null != sourceContext) {
            sourceContext.collect(sampleData);
        }

        final SampleDataHandler.WriteListener listener = success -> {
            if (success) {
                LOGGER.debug("[SampleDataHandler] response client success.");
            }else {
                LOGGER.warn("[SampleDataHandler] response client failed.");
            }
        };

        ByteBuf buf = Unpooled.wrappedBuffer("[From Server] Message received.".getBytes());
        ctx.channel().writeAndFlush(new DatagramPacket(buf, sampleData.getSender()))
                .addListener((ChannelFutureListener) future -> listener.messageRespond(future.isSuccess()));
    }

    public interface WriteListener {
        void messageRespond(boolean success);
    }
}
