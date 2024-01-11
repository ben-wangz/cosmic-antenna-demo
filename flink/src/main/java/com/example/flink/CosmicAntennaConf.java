package com.example.flink;

import java.util.Optional;
import java.util.stream.Stream;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.guava30.com.google.common.base.Preconditions;

public class CosmicAntennaConf {

  public static final ConfigOption<Integer> TIME_SAMPLE_SIZE =
      ConfigOptions.key("cosmic.antenna.app.timeSample.size")
          .intType()
          .defaultValue(16)
          .withDescription("number of time samples in one package from FPGA");
  public static final ConfigOption<Integer> TIME_SAMPLE_UNIT_SIZE =
      ConfigOptions.key("cosmic.antenna.app.timeSampleUnit.size")
          .intType()
          .defaultValue(8)
          .withDescription("minimum number of time samples in the whole system");
  public static final ConfigOption<Integer> BEAM_FORMING_WINDOW_SIZE =
      ConfigOptions.key("cosmic.antenna.app.beamForming.windowSize")
          .intType()
          .defaultValue(4)
          .withDescription("beam forming window size");
  public static final ConfigOption<Integer> FPGA_SOURCE_PARALLELISM =
      ConfigOptions.key("cosmic.antenna.app.source.parallelism")
          .intType()
          .defaultValue(1)
          .withDescription("number of parallelism for FPGA source operator");
  public static final ConfigOption<Integer> FPGA_CLIENT_DEFAULT_PORT =
      ConfigOptions.key("cosmic.antenna.app.client.default.port")
          .intType()
          .defaultValue(1080)
          .withDescription("fpga mock client default port");
  public static final ConfigOption<Integer> PACKAGE_HEADER_SIZE =
      ConfigOptions.key("cosmic.antenna.fpga.package.header.size")
          .intType()
          .defaultValue(8)
          .withDescription("data header length in one package from FPGA");
  public static final ConfigOption<Integer> CHANNEL_SIZE =
      ConfigOptions.key("cosmic.antenna.fpga.channel.size")
          .intType()
          .defaultValue(10)
          .withDescription("number of channels in one package from FPGA");
  public static final ConfigOption<Integer> ANTENNA_SIZE =
      ConfigOptions.key("cosmic.antenna.fpga.antenna.size")
          .intType()
          .defaultValue(224)
          .withDescription("number of antennas in one package from FPGA");
  public static final ConfigOption<Integer> BEAM_SIZE =
      ConfigOptions.key("cosmic.antenna.fpga.beam.size")
          .intType()
          .defaultValue(180)
          .withDescription("number of beams in one package from FPGA");
  public static final ConfigOption<String> COEFFICIENT_DATA_PATH =
      ConfigOptions.key("cosmic.antenna.coefficient.data.path")
          .stringType()
          .defaultValue(
              "C:\\Users\\Administrator\\AppData\\Local\\Temp\\cosmic-antenna4896456594933226445")
          .withDescription("path to coefficient data file");
  public static final ConfigOption<String> K8S_RESOURCE_INIT_SWITCH =
      ConfigOptions.key("cosmic.antenna.k8s.init.switch")
          .stringType()
          .defaultValue("true")
          .withDescription("if it true, will create service and endpoint");
  public static final ConfigOption<String> K8S_FLINK_NAMESPACE =
      ConfigOptions.key("cosmic.antenna.k8s.namespace")
          .stringType()
          .defaultValue("flink")
          .withDescription("flink-kubernetes-operator namespace");
  public static final ConfigOption<String> K8S_POD_ADDRESS =
      ConfigOptions.key("cosmic.antenna.k8s.pod.address")
          .stringType()
          .defaultValue("127.0.0.1")
          .withDescription("flink job pod ip address");

  public static final ConfigOption<String> JOB_NAME =
      ConfigOptions.key("cosmic.antenna.job.name")
          .stringType()
          .defaultValue("job-template-example")
          .withDescription("flink job name");

  public static class ConfigurationBuilder {
    public static Configuration build() {
      return build(new Configuration());
    }

    public static Configuration build(Configuration configuration) {
      Stream.of(
              PACKAGE_HEADER_SIZE,
              TIME_SAMPLE_SIZE,
              CHANNEL_SIZE,
              ANTENNA_SIZE,
              BEAM_SIZE,
              TIME_SAMPLE_UNIT_SIZE,
              BEAM_FORMING_WINDOW_SIZE,
              FPGA_SOURCE_PARALLELISM)
          .forEach(
              configOption ->
                  readIntegerFromEnv(
                      configuration, configOption, keyAsEnvName(configOption.key())));
      Stream.of(
              COEFFICIENT_DATA_PATH,
              K8S_FLINK_NAMESPACE,
              K8S_RESOURCE_INIT_SWITCH,
              K8S_POD_ADDRESS,
              JOB_NAME)
          .forEach(
              configOption ->
                  readStringFromEnv(configuration, configOption, keyAsEnvName(configOption.key())));
      int timeSampleSize = configuration.getInteger(TIME_SAMPLE_SIZE);
      int timeSampleUnitSize = configuration.getInteger(TIME_SAMPLE_UNIT_SIZE);
      Preconditions.checkArgument(
          timeSampleSize >= timeSampleUnitSize,
          "time sample size(%s) should be greater than or equal to time sample min size(%s)",
          timeSampleSize,
          timeSampleUnitSize);
      Preconditions.checkArgument(
          0 == timeSampleSize % timeSampleUnitSize,
          "time sample size(%s) should be divisible by time sample min size(%s)",
          timeSampleSize,
          timeSampleUnitSize);
      return configuration;
    }
  }

  private static Configuration readIntegerFromEnv(
      Configuration configuration, ConfigOption<Integer> configOption, String envName) {
    return configuration.set(
        configOption,
        Optional.ofNullable(System.getenv(envName))
            .map(Integer::parseInt)
            .orElse(configOption.defaultValue()));
  }

  private static Configuration readStringFromEnv(
      Configuration configuration, ConfigOption<String> configOption, String envName) {
    return configuration.set(
        configOption,
        Optional.ofNullable(System.getenv(envName)).orElse(configOption.defaultValue()));
  }

  private static String keyAsEnvName(String key) {
    return key.replace(".", "_");
  }
}
