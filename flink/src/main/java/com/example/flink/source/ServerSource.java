package com.example.flink.source;

import com.example.flink.CosmicAntennaConf;
import com.example.flink.data.SampleData;
import com.example.flink.source.handler.MessageDecoder;
import com.example.flink.source.handler.SampleDataHandler;
import com.example.flink.source.server.MockServer;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.netty4.io.netty.bootstrap.Bootstrap;
import org.apache.flink.shaded.netty4.io.netty.channel.*;
import org.apache.flink.shaded.netty4.io.netty.channel.group.ChannelGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.group.DefaultChannelGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.nio.NioEventLoopGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.DatagramChannel;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.flink.shaded.netty4.io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

@EqualsAndHashCode(callSuper = true)
@ToString
public class ServerSource extends RichParallelSourceFunction<SampleData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSource.class);

    private EventLoopGroup eventLoopGroup;

    private ChannelGroup defaultChannelGroup;

    private ChannelId defaultChannelId;


    @Override
    public void open(Configuration configuration) throws Exception {
        eventLoopGroup = new NioEventLoopGroup();
        defaultChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        Bootstrap serverBootstrap = new Bootstrap();
        serverBootstrap.group(eventLoopGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.AUTO_CLOSE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR,
                        new FixedRecvByteBufAllocator(configuration.get(CosmicAntennaConf.FPGA_PACKAGE_SIZE)))
                .option(ChannelOption.SO_BROADCAST, true);

        serverBootstrap.handler(new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel datagramChannel) throws Exception {
                ChannelPipeline pipeline = datagramChannel.pipeline();
                pipeline.addLast("sample-data-decoder", new MessageDecoder());
            }
        });

        int tempPort = ThreadLocalRandom.current().nextInt(10000, 65535);
        try {
            ChannelFuture channelFuture = serverBootstrap
                    .bind(tempPort)
                    .sync();
            defaultChannelId = channelFuture.channel().id();
            defaultChannelGroup.add(channelFuture.channel());
        } catch (Exception e) {
            cancel();
            throw e;
        }

        LOGGER.info("[ServerSource] sensor source inner server started at {}", tempPort);
    }

    @Override
    public void run(SourceContext<SampleData> sourceContext) throws Exception {
        SampleDataHandler sampleDataHandler = SampleDataHandler.builder()
                .sourceContext(sourceContext)
                .build();

        Channel defaultChannel = defaultChannelGroup.find(defaultChannelId);
        defaultChannel.pipeline().addLast("actual-handler", sampleDataHandler);

        LOGGER.info("[ServerSource] sensor source inner server registered a new handler");

        Thread.currentThread().join();
    }

    @Override
    public void cancel() {
        defaultChannelGroup.close();
        eventLoopGroup.shutdownGracefully();
    }

}
