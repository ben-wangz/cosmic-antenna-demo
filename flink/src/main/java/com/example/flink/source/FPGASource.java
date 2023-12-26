package com.example.flink.source;

import com.example.flink.CosmicAntennaConf;
import com.example.flink.data.AntennaData;
import com.example.flink.source.handler.MessageDecoder;
import com.example.flink.source.handler.SampleDataHandler;
import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.flink.api.common.ExecutionConfig.GlobalJobParameters;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.netty4.io.netty.bootstrap.Bootstrap;
import org.apache.flink.shaded.netty4.io.netty.channel.*;
import org.apache.flink.shaded.netty4.io.netty.channel.group.ChannelGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.group.DefaultChannelGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.nio.NioEventLoopGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.DatagramChannel;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.MessageToMessageDecoder;
import org.apache.flink.shaded.netty4.io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode(callSuper = true)
@ToString
public class FPGASource extends RichParallelSourceFunction<AntennaData> {
  private static final Logger LOGGER = LoggerFactory.getLogger(FPGASource.class);
  private static final String BLOCK_HANDLER = "BLOCK-HANDLER";

  private transient int packageHeaderSize;
  private transient int packageDataSize;
  private transient EventLoopGroup eventLoopGroup;
  private transient ChannelGroup defaultChannelGroup;
  private transient ChannelId defaultChannelId;

  @Override
  public void open(Configuration configuration) throws Exception {
    GlobalJobParameters globalJobParameters =
        getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
    Preconditions.checkArgument(
        globalJobParameters instanceof Configuration,
        "globalJobParameters(%s) is not instance of Configuration",
        globalJobParameters.getClass());
    packageHeaderSize = configuration.get(CosmicAntennaConf.PACKAGE_HEADER_SIZE);
    int timeSampleSize = configuration.get(CosmicAntennaConf.TIME_SAMPLE_SIZE);
    int channelSize = configuration.get(CosmicAntennaConf.CHANNEL_SIZE);
    packageDataSize = timeSampleSize * channelSize * 2;
    eventLoopGroup = new NioEventLoopGroup();
    defaultChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    Bootstrap serverBootstrap =
        new Bootstrap()
            .group(eventLoopGroup)
            .channel(NioDatagramChannel.class)
            .option(ChannelOption.AUTO_CLOSE, true)
            .option(
                ChannelOption.RCVBUF_ALLOCATOR,
                new FixedRecvByteBufAllocator(packageDataSize + packageHeaderSize))
            .option(ChannelOption.SO_BROADCAST, true);
    serverBootstrap.handler(
        new ChannelInitializer<DatagramChannel>() {
          @Override
          protected void initChannel(DatagramChannel datagramChannel) throws Exception {
            ChannelPipeline pipeline = datagramChannel.pipeline();
            pipeline.addLast(
                BLOCK_HANDLER,
                new MessageToMessageDecoder<>() {
                  @Override
                  protected void decode(ChannelHandlerContext ctx, Object obj, List<Object> out)
                      throws Exception {
                    Thread.currentThread().wait();
                  }
                });
          }
        });
    ChannelFuture channelFuture = serverBootstrap.bind(0).sync();
    LOGGER.info(
        "inner netty server started at {}",
        ((InetSocketAddress) channelFuture.channel().localAddress()).getPort());

    defaultChannelId = channelFuture.channel().id();
    defaultChannelGroup.add(channelFuture.channel());
  }

  @Override
  public void run(SourceContext<AntennaData> sourceContext) throws Exception {
    ChannelPipeline channelPipeline = defaultChannelGroup.find(defaultChannelId).pipeline();
    channelPipeline.remove(BLOCK_HANDLER);
    LOGGER.info("inner netty server unregistered the blocking handler");
    String decoderIdentifier = "sample-data-decoder";
    channelPipeline.addLast(
        decoderIdentifier,
        MessageDecoder.builder().headerSize(packageHeaderSize).dataSize(packageDataSize).build());
    LOGGER.info("inner netty server registered \"{}\"", decoderIdentifier);
    final String byteDataHandlerIdentifier = "byte-data-handler";
    channelPipeline.addLast(
        byteDataHandlerIdentifier,
        SampleDataHandler.builder().sourceContext(sourceContext).dataSize(packageDataSize).build());
    LOGGER.info("inner netty server registered \"{}\"", byteDataHandlerIdentifier);
    // TODO block thread with another way
    Thread.currentThread().join();
  }

  @Override
  public void cancel() {
    if (null != defaultChannelGroup) {
      defaultChannelGroup.close();
    }
    if (null != eventLoopGroup) {
      eventLoopGroup.shutdownGracefully();
    }
  }
}
