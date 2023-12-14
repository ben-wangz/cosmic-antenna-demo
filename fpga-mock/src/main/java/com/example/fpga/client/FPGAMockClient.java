package com.example.fpga.client;

import com.example.fpga.handler.SampleDataHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;


public class FPGAMockClient {
    int port;
    Channel channel;
    EventLoopGroup workGroup = new NioEventLoopGroup();

    public FPGAMockClient(int port) {
        this.port = port;
    }

    public ChannelFuture startup(String host) throws Exception {
        try {
            Bootstrap b = new Bootstrap();
            b.group(workGroup);
            b.channel(NioDatagramChannel.class);
            b.handler(new ChannelInitializer<DatagramChannel>() {
                protected void initChannel(DatagramChannel datagramChannel) {
                    datagramChannel.pipeline()
                            .addLast(new SampleDataHandler());
                }
            });
            ChannelFuture channelFuture = b.connect(host, this.port).sync();
            this.channel = channelFuture.channel();
            return channelFuture;
        } finally {
        }
    }

    public void shutdown() {
        workGroup.shutdownGracefully();
    }
}
