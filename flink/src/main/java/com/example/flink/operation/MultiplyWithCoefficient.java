package com.example.flink.operation;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.opencv.opencv_core.Mat;

import com.example.flink.CosmicAntennaConf;
import com.example.flink.data.CoefficientData;
import com.example.flink.data.SampleData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

public class MultiplyWithCoefficient extends ProcessWindowFunction<SampleData, SampleData, Integer, TimeWindow> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private transient Integer timeSampleUnitSize;
    private transient Integer antennaSize;

    @Override
    public void open(Configuration configuration) throws Exception {
        timeSampleUnitSize = configuration.get(CosmicAntennaConf.TIME_SAMPLE_UNIT_SIZE);
        antennaSize = configuration.get(CosmicAntennaConf.ANTENNA_SIZE);
        CoefficientData coefficientData = OBJECT_MAPPER.readValue("", new TypeReference<CoefficientData>() {
        });
        // TODO read coefficient from configuration
    };

    @Override
    public void process(
            Integer channelId,
            ProcessWindowFunction<SampleData, SampleData, Integer, TimeWindow>.Context context,
            Iterable<SampleData> iterable,
            Collector<SampleData> collector) throws Exception {
        // all SampleData of all antenna will be received
        // if not, interpret the missing data with 0
        try (Mat realMat = new Mat(
                antennaSize,
                timeSampleUnitSize,
                org.bytedeco.opencv.global.opencv_core.CV_64FC1);
                Mat imaginaryMat = new Mat(
                        antennaSize,
                        timeSampleUnitSize,
                        org.bytedeco.opencv.global.opencv_core.CV_64FC1)) {
            Indexer realMatIndexer = realMat.createIndexer();
            Indexer imaginaryMatIndexer = imaginaryMat.createIndexer();
            for (SampleData sampleData : iterable) {
                int antennaId = sampleData.getAntennaId();
                byte[] realArray = sampleData.getRealArray();
                byte[] imaginaryArray = sampleData.getImaginaryArray();
                for (int index = 0; index < realArray.length; index++) {
                    long[] position = new long[] { antennaId, index };
                    realMatIndexer.putDouble(position, realArray[index]);
                    imaginaryMatIndexer.putDouble(position, imaginaryArray[index]);
                }
            }
            // TODO multiply with coefficient
            org.bytedeco.opencv.global.opencv_core.multiply(realMat, imaginaryMat);
        }
    }
}
