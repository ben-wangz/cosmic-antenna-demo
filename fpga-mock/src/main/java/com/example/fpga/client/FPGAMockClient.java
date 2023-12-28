package com.example.fpga.client;

import com.example.fpga.handler.SampleDataHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.io.Closeable;
import java.io.IOException;
import lombok.Builder;

@Builder
public class FPGAMockClient implements Closeable {
  int port;
  @Builder.Default EventLoopGroup workGroup = new NioEventLoopGroup();

  public ChannelFuture startup(String host) throws Exception {
    try {
      Bootstrap b = new Bootstrap();
      b.group(workGroup);
      b.channel(NioDatagramChannel.class);
      b.handler(
          new ChannelInitializer<DatagramChannel>() {
            protected void initChannel(DatagramChannel datagramChannel) {
              datagramChannel.pipeline().addLast(new SampleDataHandler());
            }
          });
      return b.connect(host, this.port).sync();
    } finally {
    }
  }

  @Override
  public void close() throws IOException {
    workGroup.shutdownGracefully();
  }
}
