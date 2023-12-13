package com.example.flink.source.handler;

import com.example.flink.data.SampleData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.SimpleChannelInboundHandler;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Builder
@AllArgsConstructor
public class ByteDataHandler extends SimpleChannelInboundHandler<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ByteDataHandler.class);

    private SourceFunction.SourceContext<SampleData> sourceContext;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        LOGGER.error(cause.getLocalizedMessage());
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) throws Exception {
        Object deserialized = SerializationUtils.deserialize(bytes);
        LOGGER.warn("[ByteDataHandler] Message Received : " + deserialized);
        if (null != sourceContext) {
            sourceContext.collect((SampleData) deserialized);
        }
    }
}
