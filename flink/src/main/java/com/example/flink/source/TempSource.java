package com.example.flink.source;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
public class TempSource extends RichParallelSourceFunction<SampleData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempSource.class);

    private transient Integer channelId;
    private transient Integer antennaSize;
    private transient Integer timeSampleSize;
    private transient AtomicLong counter;
    private transient Long sleepTimeInterval;
    private boolean running = true;

    @Override
    public void open(Configuration configuration) throws Exception {
        // mock channel id as task index
        channelId = getRuntimeContext().getIndexOfThisSubtask();
        antennaSize = configuration.get(CosmicAntennaConf.ANTENNA_SIZE);
        timeSampleSize = configuration.get(CosmicAntennaConf.TIME_SAMPLE_SIZE);
        counter = new AtomicLong(configuration.get(CosmicAntennaConf.START_COUNTER));
        sleepTimeInterval = configuration.get(CosmicAntennaConf.SLEEP_TIME_INTERVAL);
    }

    @Override
    public void run(SourceContext<SampleData> sourceContext) throws Exception {
        Random random = new Random();
        while (running) {
            byte[] realArray = new byte[timeSampleSize];
            byte[] imaginaryArray = new byte[timeSampleSize];
            random.nextBytes(realArray);
            random.nextBytes(imaginaryArray);
            for (int index = 0; index < antennaSize; index++) {
                SampleData sampleData = SampleData.builder()
                        .channelId(channelId)
                        .antennaId(index)
                        .startCounter(counter.getAndIncrement())
                        .realArray(realArray)
                        .imaginaryArray(imaginaryArray)
                        .build();
                LOGGER.info("[Temp Source] Message Generated : {}", sampleData);
                sourceContext.collect(sampleData);
            }
            TimeUnit.MILLISECONDS.sleep(sleepTimeInterval);
        }
    }

    @Override
    public void cancel() {
        this.running = false;
    }
}