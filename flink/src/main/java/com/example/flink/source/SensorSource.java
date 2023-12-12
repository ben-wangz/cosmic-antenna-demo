package com.example.flink.source;

import com.example.flink.source.handler.SampleDataHandler;
import com.example.flink.source.server.MockServer;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

import com.example.flink.CosmicAntennaConf;
import com.example.flink.data.SampleData;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode(callSuper = true)
@ToString
public class SensorSource extends RichParallelSourceFunction<SampleData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SensorSource.class);

    private boolean running = true;
    private MockServer mockServer;

    @Override
    public void open(Configuration configuration) throws Exception {
        mockServer = new MockServer();
        mockServer.startup(configuration.get(CosmicAntennaConf.FPGA_SERVER_PORT));
        LOGGER.info("[SensorSource] sensor source inner server started at {}", configuration.get(CosmicAntennaConf.FPGA_SERVER_PORT));
    }

    @Override
    public void run(SourceContext<SampleData> sourceContext) throws Exception {
        SampleDataHandler sampleDataHandler = SampleDataHandler.builder()
                .sourceContext(sourceContext)
                .build();
        sourceContext.markAsTemporarilyIdle();

        mockServer.registerHandler("actual-handler", sampleDataHandler);
        LOGGER.info("[SensorSource] sensor source inner server registered a new handler");
    }

    @Override
    public void cancel() {
        this.running = false;
        try {
            mockServer.shutdown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
