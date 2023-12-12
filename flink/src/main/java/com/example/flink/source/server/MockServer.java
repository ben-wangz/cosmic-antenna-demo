package com.example.flink.source.server;

import com.example.flink.source.SensorSource;
import com.example.flink.source.handler.MessageDecoder;
import org.apache.flink.shaded.netty4.io.netty.bootstrap.Bootstrap;
import org.apache.flink.shaded.netty4.io.netty.channel.*;
import org.apache.flink.shaded.netty4.io.netty.channel.group.ChannelGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.group.DefaultChannelGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.nio.NioEventLoopGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.DatagramChannel;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.flink.shaded.netty4.io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockServer.class);
    private final EventLoopGroup bossLoopGroup;

    private final ChannelGroup defaultChannelGroup;

    private ChannelId defaultChannelId;


    public MockServer() {
        this.bossLoopGroup = new NioEventLoopGroup();
        this.defaultChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    }

    /**
     * Initialize the UDP server
     * @param port
     * @throws Exception
     */
    public final void startup(int port) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(bossLoopGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.AUTO_CLOSE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BROADCAST, true);

        bootstrap.handler(new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel datagramChannel) throws Exception {
                ChannelPipeline pipeline = datagramChannel.pipeline();
                pipeline.addLast("sample-data-decoder", new MessageDecoder());
            }
        });

        try {
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            defaultChannelId = channelFuture.channel().id();
            defaultChannelGroup.add(channelFuture.channel());
        } catch (Exception e) {
            shutdown();
            throw e;
        }
    }

    public final void registerHandler(String handlerName, ChannelHandler channelHandler){
        Channel defaultChannel = defaultChannelGroup.find(defaultChannelId);
        ChannelPipeline pipeline = defaultChannel.pipeline();
        pipeline.addLast(handlerName, channelHandler);
    }

    /**
     * Shutdown the server
     *
     * @throws Exception
     */
    public final void shutdown() throws Exception {
        defaultChannelGroup.close();
        bossLoopGroup.shutdownGracefully();
    }
}