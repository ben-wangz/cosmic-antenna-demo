package com.example.flink.operation;

import static org.bytedeco.opencv.global.opencv_core.CV_64FC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_UNCHANGED;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;

import com.example.flink.CosmicAntennaConf;
import com.example.flink.data.BeamData;
import com.example.flink.data.CoefficientData;
import com.example.flink.data.SampleData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MultiplyWithCoefficient
                extends ProcessWindowFunction<SampleData, BeamData, Integer, TimeWindow> {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        private transient Integer timeSampleUnitSize;
        private transient Integer antennaSize;
        private transient Integer beamSize;
        private Map<Integer, CoefficientData> coefficientDataMap;

        @Override
        public void open(Configuration configuration) throws Exception {
                timeSampleUnitSize = configuration.get(CosmicAntennaConf.TIME_SAMPLE_UNIT_SIZE);
                antennaSize = configuration.get(CosmicAntennaConf.ANTENNA_SIZE);
                beamSize = configuration.get(CosmicAntennaConf.BEAM_SIZE);
                // String tempFilePath =
                // configuration.get(CosmicAntennaConf.COEFFICIENT_DATA_PATH);
                String tempFilePath = "C:\\Users\\Administrator\\AppData\\Local\\Temp\\coefficient-11893722159847825320.json";
                List<CoefficientData> coefficientDataList = OBJECT_MAPPER.readValue(
                                Paths.get(tempFilePath).toFile(), new TypeReference<>() {
                                });
                coefficientDataMap = coefficientDataList.stream()
                                .collect(Collectors.toMap(CoefficientData::getChannelId, Function.identity()));
        }

        @Override
        public void process(Integer channelId,
                        ProcessWindowFunction<SampleData, BeamData, Integer, TimeWindow>.Context context,
                        Iterable<SampleData> iterable, Collector<BeamData> collector)
                        throws Exception {
                // all SampleData of all antenna will be received if not, interpret the missing
                // data with 0
                CoefficientData coefficientData = coefficientDataMap.get(channelId);
                try (Mat dataRealMat = new Mat(antennaSize, timeSampleUnitSize, CV_64FC1);
                                Mat dataImaginaryMat = new Mat(antennaSize, timeSampleUnitSize, CV_64FC1);
                                Mat coefficientRealMat = imdecode(new Mat(coefficientData.getRealArray()),
                                                IMREAD_UNCHANGED);
                                Mat coefficientImaginaryMat = imdecode(
                                                new Mat(coefficientData.getImaginaryArray()),
                                                IMREAD_UNCHANGED)) {
                        coefficientRealMat.create(beamSize, antennaSize, CV_64FC1);
                        coefficientImaginaryMat.create(beamSize, antennaSize, CV_64FC1);
                        Indexer realMatIndexer = dataRealMat.createIndexer();
                        Indexer imaginaryMatIndexer = dataImaginaryMat.createIndexer();
                        for (SampleData sampleData : iterable) {
                                int antennaId = sampleData.getAntennaId();
                                byte[] realArray = sampleData.getRealArray();
                                System.out.println(Arrays.toString(realArray));
                                byte[] imaginaryArray = sampleData.getImaginaryArray();
                                for (int index = 0; index < realArray.length; index++) {
                                        long[] position = new long[] { antennaId, index };
                                        realMatIndexer.putDouble(position, realArray[index]);
                                        imaginaryMatIndexer.putDouble(position,
                                                        imaginaryArray[index]);
                                }
                        }
                        // beamSize * timeSampleUnitSize (180 * 64)
                        Mat mat = opencv_core.add(opencv_core.add(
                                        opencv_core.multiply(coefficientRealMat, dataRealMat),
                                        opencv_core.multiply(coefficientRealMat, dataImaginaryMat)),
                                        opencv_core.add(opencv_core.multiply(
                                                        coefficientImaginaryMat, dataRealMat),
                                                        opencv_core.multiply(
                                                                        coefficientImaginaryMat,
                                                                        dataImaginaryMat)))
                                        .asMat();
                        IntStream.range(1, mat.rows() + 1).boxed().forEach(beamId -> {
                                byte[] tempBytes = new byte[timeSampleUnitSize];
                                mat.row(beamId - 1).data().get(tempBytes);
                                collector.collect(BeamData.builder().beamId(beamId)
                                                .channelId(channelId).resultArray(tempBytes)
                                                .build());
                        });
                }
        }
}
