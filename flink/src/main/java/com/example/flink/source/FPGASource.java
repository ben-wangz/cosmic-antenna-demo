package com.example.flink.source;

import com.example.flink.CosmicAntennaConf;
import com.example.flink.data.AntennaData;
import com.example.flink.source.handler.MessageDecoder;
import com.example.flink.source.handler.SampleDataHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import java.net.InetSocketAddress;
import java.util.List;
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
  private transient String flinkNameSpace;

  @Override
  public void open(Configuration configuration) throws Exception {
    GlobalJobParameters globalJobParameters =
        getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
    Preconditions.checkArgument(
        globalJobParameters instanceof Configuration,
        "globalJobParameters(%s) is not instance of Configuration",
        globalJobParameters.getClass());
    initSwitch =
        ((Configuration) globalJobParameters).get(CosmicAntennaConf.K8S_RESOURCE_INIT_SWITCH);
    flinkNameSpace =
        ((Configuration) globalJobParameters).get(CosmicAntennaConf.K8S_FLINK_NAMESPACE);
    packageHeaderSize =
        ((Configuration) globalJobParameters).get(CosmicAntennaConf.PACKAGE_HEADER_SIZE);
    int timeSampleSize =
        ((Configuration) globalJobParameters).get(CosmicAntennaConf.TIME_SAMPLE_SIZE);
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
    int serverPort = ((InetSocketAddress) channelFuture.channel().localAddress()).getPort();

    LOGGER.info("inner netty server started port: {}", serverPort);

    defaultChannelId = channelFuture.channel().id();
    defaultChannelGroup.add(channelFuture.channel());

    if (Boolean.parseBoolean(initSwitch)) {
      String ipAddr =
          Optional.ofNullable(
                  System.getenv(CosmicAntennaConf.K8S_POD_ADDRESS.key().replaceAll("\\.", "_")))
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "cannot find environment variable \"cosmic_antenna_k8s_pod_address\""));
      String jobName = ((Configuration) globalJobParameters).get(CosmicAntennaConf.JOB_NAME);
      Integer clientPort =
          ((Configuration) globalJobParameters).get(CosmicAntennaConf.FPGA_CLIENT_DEFAULT_PORT);
      initK8sResources(
          jobName, getRuntimeContext().getIndexOfThisSubtask(), ipAddr, clientPort, serverPort);
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

  private void initK8sResources(
      String jobName, int sourceId, String ipAddr, int port, int targetPort)
      throws JsonProcessingException {
    String resourceName = String.format("%s-fpga-server-%s", jobName, sourceId);
    String portName = "fpga";

    LOGGER.info("going to init k8s endpoint and service resource.");
    try (KubernetesClient kubernetesClient = new KubernetesClientBuilder().build()) {
      Service service =
          new ServiceBuilder()
              .withNewMetadata()
              .withName(resourceName)
              .withNamespace(flinkNameSpace)
              .endMetadata()
              .withNewSpec()
              .addNewPort()
              .withName(portName)
              .withProtocol("UDP")
              .withPort(port)
              .withTargetPort(new IntOrString(targetPort))
              .endPort()
              .endSpec()
              .build();
      LOGGER.info(
          "going to init service yaml -> {}",
          Serialization.yamlMapper().writeValueAsString(service));
      kubernetesClient.services().inNamespace(flinkNameSpace).resource(service).createOrReplace();
      Endpoints endpoints =
          new EndpointsBuilder()
              .withNewMetadata()
              .withName(resourceName)
              .withNamespace(flinkNameSpace)
              .endMetadata()
              .withSubsets()
              .addNewSubset()
              .addNewAddress()
              .withIp(ipAddr)
              .endAddress()
              .addNewPort()
              .withName(portName)
              .withPort(targetPort)
              .withProtocol("UDP")
              .endPort()
              .endSubset()
              .build();
      LOGGER.info(
          "going to init endpoint yaml -> {}",
          Serialization.yamlMapper().writeValueAsString(endpoints));
      kubernetesClient
          .endpoints()
          .inNamespace(flinkNameSpace)
          .resource(endpoints)
          .createOrReplace();
    }
  }
}
