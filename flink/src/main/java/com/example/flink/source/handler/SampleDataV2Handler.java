package com.example.flink.source.handler;

import com.example.flink.data.SampleData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.SimpleChannelInboundHandler;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

@Builder
@AllArgsConstructor
public class SampleDataV2Handler extends SimpleChannelInboundHandler<byte[]> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataV2Handler.class);
    private SourceFunction.SourceContext<SampleData> sourceContext;

    @Builder.Default
    private int dataChunkSize = 2048;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, byte[] bytes) throws Exception {
        ByteBuffer antennaId = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, 1));
        ByteBuffer counter = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 2, 8));

        byte[] paddedCounter = new byte[8];
        System.arraycopy(counter.array(), 0, paddedCounter, 0, counter.array().length);

        ByteBuffer realArray = ByteBuffer.wrap(new byte[dataChunkSize]);
        ByteBuffer imaginaryArray = ByteBuffer.wrap(new byte[dataChunkSize]);
        for (int i = 12; i < bytes.length; i++) {
            if (i % 2 == 0) {
                realArray.put(bytes[i]);
            } else {
                imaginaryArray.put(bytes[i]);
            }
        }
        SampleData sampleData = SampleData.builder()
                .antennaId(antennaId.get() & 0xFF)
                .startCounter(ByteBuffer.wrap(paddedCounter).getLong())
                .realArray(realArray.array())
                .imaginaryArray(imaginaryArray.array())
                .build();
        LOGGER.info("Message received : " + sampleData);
        if (null != sourceContext) {
            sourceContext.collect(sampleData);
        }
    }
}
