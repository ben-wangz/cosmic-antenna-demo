package com.example.flink;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

public class CosmicAntennaConf {
//    public static final ConfigOption<Integer> TIME_SAMPLE_SIZE = ConfigOptions
//            .key("cosmic.antenna.timeSampleSize")
//            .intType()
//            .defaultValue(400);
    public static final ConfigOption<Integer> DATA_CHUNK_SIZE = ConfigOptions
            .key("cosmic.antenna.dataChunkSize")
            .intType()
            .defaultValue(4000);
    public static final ConfigOption<Integer> DATA_HEADER_SIZE = ConfigOptions
            .key("cosmic.antenna.dataHeaderSize")
            .intType()
            .defaultValue(8);
    public static final ConfigOption<Integer> TIME_SAMPLE_UNIT_SIZE = ConfigOptions
            .key("cosmic.antenna.timeSampleUnitSize")
            .intType()
            .defaultValue(8);
    public static final ConfigOption<Integer> ANTENNA_SIZE = ConfigOptions
            .key("cosmic.antenna.antennaSize")
            .intType()
            .defaultValue(224);
    public static final ConfigOption<Integer> BEAM_SIZE = ConfigOptions
            .key("cosmic.antenna.beamSize")
            .intType()
            .defaultValue(180);
    public static final ConfigOption<Long> START_COUNTER = ConfigOptions
            .key("cosmic.antenna.startCounter")
            .longType()
            .defaultValue(0L);
    public static final ConfigOption<Long> SLEEP_TIME_INTERVAL = ConfigOptions
            .key("cosmic.antenna.sleepTimeInterval")
            .longType()
            .defaultValue(1000L);
    public static final ConfigOption<Integer> FPGA_PACKAGE_SIZE = ConfigOptions
            .key("cosmic.antenna.fpga.package.size")
            .intType()
            .defaultValue(DATA_CHUNK_SIZE.defaultValue() * 2 + DATA_HEADER_SIZE.defaultValue());
    public static final ConfigOption<String> COEFFICIENT_DATA_PATH = ConfigOptions
            .key("cosmic.antenna.coefficient.data.path")
            .stringType()
            .defaultValue("");
}
