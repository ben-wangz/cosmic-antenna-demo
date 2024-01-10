package com.example.flink.source;

import com.example.flink.CosmicAntennaConf;
import com.example.flink.data.AntennaData;
import com.example.flink.source.handler.MessageDecoder;
import com.example.flink.source.handler.SampleDataHandler;
import com.google.common.base.Preconditions;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private static final long serialVersionUID = -4102927494134535194L;

  private transient int packageHeaderSize;
  private transient int packageDataSize;
  private transient EventLoopGroup eventLoopGroup;
  private transient ChannelGroup defaultChannelGroup;
  private transient ChannelId defaultChannelId;
  private transient String initSwitch;
  private transient String flinkResourceNameSpace;

  @Override
  public void open(Configuration configuration) throws Exception {
    GlobalJobParameters globalJobParameters =
        getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
    Preconditions.checkArgument(
        globalJobParameters instanceof Configuration,
        "globalJobParameters(%s) is not instance of Configuration",
        globalJobParameters.getClass());
    initSwitch = ((Configuration) globalJobParameters).get(CosmicAntennaConf.K8S_RESOURCE_INIT_SWITCH);
    flinkResourceNameSpace = ((Configuration) globalJobParameters).get(CosmicAntennaConf.K8S_FLINK_RESOURCE_NAMESPACE);
    packageHeaderSize = ((Configuration) globalJobParameters).get(CosmicAntennaConf.PACKAGE_HEADER_SIZE);
    int timeSampleSize = ((Configuration) globalJobParameters).get(CosmicAntennaConf.TIME_SAMPLE_SIZE);
    int channelSize = ((Configuration) globalJobParameters).get(CosmicAntennaConf.CHANNEL_SIZE);
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
    int port = ((InetSocketAddress) channelFuture.channel().localAddress()).getPort();
    String ipAddr =
        Optional.ofNullable(System.getenv("cosmic_antenna_k8s_pod_address"))
            .orElse(
                ((InetSocketAddress) channelFuture.channel().localAddress())
                    .getAddress()
                    .getHostAddress());
    LOGGER.info("inner netty server started at address: {}, port: {}", ipAddr, port);

    defaultChannelId = channelFuture.channel().id();
    defaultChannelGroup.add(channelFuture.channel());

    if (Boolean.parseBoolean(initSwitch)) {
      initK8sResources(getRuntimeContext().getIndexOfThisSubtask(), ipAddr, port);
    } else {
      LOGGER.warn("this app is not running in k8s cluster. dont need to create k8s resources.");
    }
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

  private void initK8sResources(int sourceId, String ipAddr, int port) {
    String resourceName = String.format("job-template-example-fpga-server-%s", sourceId);
    String portName = "http";

    Map<String, String> singletonMap =
        Collections.singletonMap("app.source.service.endpoint/name", getRuntimeContext().getJobId() + "-" + sourceId);
    LOGGER.info("going to init k8s endpoint and service resource.");
    try (KubernetesClient kubernetesClient = new KubernetesClientBuilder().build()) {
      Service service =
          new ServiceBuilder()
              .withNewMetadata()
              .withName(resourceName)
              .withNamespace(flinkResourceNameSpace)
              .endMetadata()
              .withNewSpec()
              .withSelector(singletonMap)
              .addNewPort()
              .withName(portName)
              .withProtocol("UDP")
              .withPort(port)
              .withTargetPort(new IntOrString(port))
              .endPort()
              .endSpec()
              .build();
      LOGGER.info(
          "going to init service yaml -> {}",
          Serialization.yamlMapper().writeValueAsString(service));
      kubernetesClient.services().inNamespace(flinkResourceNameSpace).resource(service).create();
      Endpoints endpoints =
          new EndpointsBuilder()
              .withNewMetadata()
              .withName(resourceName + "-endpoint")
              .withNamespace(flinkResourceNameSpace)
              .withLabels(singletonMap)
              .endMetadata()
              .withSubsets()
              .addNewSubset()
              .addNewAddress()
              .withIp(ipAddr)
              .endAddress()
              .addNewPort()
              .withName(portName)
              .withPort(port)
              .endPort()
              .endSubset()
              .build();
      LOGGER.info(
          "going to init endpoint yaml -> {}",
          Serialization.yamlMapper().writeValueAsString(endpoints));
      kubernetesClient.endpoints().inNamespace(flinkResourceNameSpace).resource(endpoints).create();
    } catch (Exception e) {
      LOGGER.error("init k8s resource failed. since {}", e.getCause().getMessage());
    }
  }
}
