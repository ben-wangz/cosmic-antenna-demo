package com.example.flink.operation;

import com.example.flink.data.ChannelBeamData;
import com.example.flink.data.ChannelData;
import com.example.flink.data.CoefficientData;
import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode
@ToString
public class BeamFormingWindowFunction
    implements WindowFunction<ChannelData, ChannelBeamData, Integer, TimeWindow>, Closeable {
  private static final long serialVersionUID = 5101076627162003094L;
  private static final Logger LOGGER = LoggerFactory.getLogger(BeamFormingWindowFunction.class);
  private final Integer channelSize;
  private final Integer beamSize;
  private final Integer antennaSize;
  private final Integer timeSampleUnitSize;
  private final Integer beamFormingWindowSize;
  private final List<CoefficientData> coefficientDataList;
  private transient Mat coefficientRealMat;
  private transient Mat coefficientImaginaryMat;

  @Builder
  @Jacksonized
  public BeamFormingWindowFunction(
      Integer channelSize,
      Integer beamSize,
      Integer antennaSize,
      Integer timeSampleUnitSize,
      Integer beamFormingWindowSize,
      List<CoefficientData> coefficientDataList) {
    this.channelSize = channelSize;
    this.beamSize = beamSize;
    this.antennaSize = antennaSize;
    this.timeSampleUnitSize = timeSampleUnitSize;
    this.beamFormingWindowSize = beamFormingWindowSize;
    this.coefficientDataList = coefficientDataList;
    Preconditions.checkArgument(
        coefficientDataList.size() == channelSize,
        "coefficientDataList.size(%s) == channelSize(%s)",
        coefficientDataList.size(),
        channelSize);
  }

  @Override
  public void apply(
      Integer channelId,
      TimeWindow window,
      Iterable<ChannelData> channelDataIterable,
      Collector<ChannelBeamData> collector)
      throws Exception {
    long startCounterOfWindow = window.getStart();
    Map<Long, ChannelData> indexedChannelData =
        StreamSupport.stream(channelDataIterable.spliterator(), false)
            .collect(Collectors.toMap(ChannelData::getCounter, Function.identity()));
    int length = antennaSize * timeSampleUnitSize * beamFormingWindowSize;
    byte[] realArray = new byte[length];
    byte[] imaginaryArray = new byte[length];
    for (int unitIndex = 0; unitIndex < beamFormingWindowSize; unitIndex++) {
      for (int antennaIndex = 0; antennaIndex < antennaSize; antennaIndex++) {
        int startIndexOfMergedChannelData =
            antennaIndex * timeSampleUnitSize * beamFormingWindowSize
                + unitIndex * timeSampleUnitSize;
        LOGGER.debug(
            "beam forming split index from merged antenna {}, data -> {}",
            antennaIndex,
            startIndexOfMergedChannelData);
        int startIndexOfUnitChannelData = antennaIndex * timeSampleUnitSize;
        LOGGER.debug(
            "beam forming split index from unit antenna {}, data -> {}",
            antennaIndex,
            startIndexOfUnitChannelData);
        ChannelData unitChannelData = indexedChannelData.get(startCounterOfWindow + unitIndex);
        // missing data will be interpreted with 0
        if (null == unitChannelData) {
          LOGGER.warn(
              "cannot get indexed channel data{}, since indexedChannelData.keys -> {}",
              startCounterOfWindow + unitIndex,
              indexedChannelData.keySet());
          continue;
        }
        LOGGER.debug(
            "unitChannelData data length -> {}", unitChannelData.getRealArray().length); // 1792
        System.arraycopy(
            unitChannelData.getRealArray(),
            startIndexOfUnitChannelData,
            realArray,
            startIndexOfMergedChannelData,
            timeSampleUnitSize);
        System.arraycopy(
            unitChannelData.getImaginaryArray(),
            startIndexOfUnitChannelData,
            imaginaryArray,
            startIndexOfMergedChannelData,
            timeSampleUnitSize);
      }
    }
    loadCoefficientMats(channelId);
    LOGGER.debug(
        "load coefficient matrix-{} success. coefficientRealMat({},{}) has {} elements.",
        channelId,
        coefficientRealMat.rows(),
        coefficientRealMat.cols(),
        coefficientRealMat.total());
    ChannelData mergedChannelData =
        ChannelData.builder()
            .channelId(channelId)
            .counter(startCounterOfWindow)
            .realArray(realArray)
            .imaginaryArray(imaginaryArray)
            .build();
    try (Mat dataRealMat = new Mat(antennaSize, timeSampleUnitSize, opencv_core.CV_64FC1);
        Mat dataImaginaryMat = new Mat(antennaSize, timeSampleUnitSize, opencv_core.CV_64FC1)) {

      dataRealMat.data().put(mergedChannelData.getRealArray());
      dataImaginaryMat.data().put(mergedChannelData.getImaginaryArray());
      LOGGER.debug(
          "created a data real Mat({}, {}), containing {} elements.",
          dataRealMat.rows(),
          dataRealMat.cols(),
          dataRealMat.total()); // 224, 8
      LOGGER.debug(
          "created a data imaginary Mat({}, {}), containing {} elements.",
          dataImaginaryMat.rows(),
          dataImaginaryMat.cols(),
          dataImaginaryMat.total());
      Mat realMat =
          opencv_core
              .add(
                  opencv_core.multiply(coefficientRealMat, dataRealMat),
                  opencv_core.multiply(coefficientImaginaryMat, dataImaginaryMat))
              .asMat();
      Mat imaginaryMat =
          opencv_core
              .add(
                  opencv_core.multiply(coefficientRealMat, dataImaginaryMat),
                  opencv_core.multiply(coefficientImaginaryMat, dataRealMat))
              .asMat();
      LOGGER.debug(
          "created a result real Mat({}, {}), containing {} elements.",
          realMat.rows(),
          realMat.cols(),
          realMat.total()); // 180, 8
      LOGGER.debug(
          "created a result imaginary Mat({}, {}), containing {} elements.",
          imaginaryMat.rows(),
          imaginaryMat.cols(),
          imaginaryMat.total());
      int channelBeamDataLength = timeSampleUnitSize * beamFormingWindowSize;
      Preconditions.checkArgument(
          realMat.rows() == beamSize, "realMat.rows(%s) != beamSize(%s)", realMat.rows(), beamSize);
      Preconditions.checkArgument(
          realMat.cols() == timeSampleUnitSize,
          "realMat.cols(%s) != timeSampleUnitSize(%s)",
          realMat.cols(),
          channelBeamDataLength);
      Preconditions.checkArgument(
          imaginaryMat.rows() == beamSize,
          "imaginaryMat.rows(%s) != beamSize(%s)",
          imaginaryMat.rows(),
          beamSize);
      Preconditions.checkArgument(
          imaginaryMat.cols() == timeSampleUnitSize,
          "imaginaryMat.cols(%s) != timeSampleUnitSize(%s)",
          imaginaryMat.cols(),
          channelBeamDataLength);
      for (int beamIndex = 0; beamIndex < beamSize; beamIndex++) {
        byte[] realArrayOfChannelBeamData = new byte[channelBeamDataLength];
        realMat.row(beamIndex).data().get(realArrayOfChannelBeamData);
        byte[] imaginaryArrayOfChannelBeamData = new byte[channelBeamDataLength];
        imaginaryMat.row(beamIndex).data().get(imaginaryArrayOfChannelBeamData);
        collector.collect(
            ChannelBeamData.builder()
                .channelId(channelId)
                .beamId(beamIndex)
                .realArray(realArrayOfChannelBeamData)
                .imaginaryArray(imaginaryArrayOfChannelBeamData)
                .build());
      }
    }
  }

  private void loadCoefficientMats(Integer channelId) {
    if (null == coefficientRealMat) {
      coefficientRealMat = new Mat(beamSize, antennaSize, opencv_core.CV_64FC1);
      coefficientRealMat.data().put(coefficientDataList.get(channelId).getRealArray());
    }
    if (null == coefficientImaginaryMat) {
      coefficientImaginaryMat = new Mat(beamSize, antennaSize, opencv_core.CV_64FC1);
      coefficientImaginaryMat.data().put(coefficientDataList.get(channelId).getImaginaryArray());
    }
  }

  @Override
  public void close() {
    if (null != coefficientRealMat) {
      coefficientRealMat.close();
    }
    if (null != coefficientImaginaryMat) {
      coefficientImaginaryMat.close();
    }
  }
}
