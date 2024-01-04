package com.example.flink;

import java.util.Random;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatrixMultiplyTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(MatrixMultiplyTest.class);

  private static final Integer row = 224;
  private static final Integer col = 8;

  private static final Integer channel = 2;

  private static final Random random = new Random(4564);

  private static void printMatInfo(Mat mat) {
    LOGGER.info(
        "Mat({}, {}), containing {} channels and each has {} elements, {} in total;",
        mat.rows(),
        mat.cols(),
        mat.channels(),
        mat.total(),
        mat.channels() * mat.total());
  }

  @Test
  public void testAdd() {

    try (Mat aMat = new Mat(row, col, opencv_core.CV_64FC(channel));
        Mat bMat = new Mat(row, col, opencv_core.CV_64FC(channel))) {
      byte[] abytes = new byte[row * col * channel];
      random.nextBytes(abytes);
      aMat.data().put(abytes);

      byte[] bbytes = new byte[row * col * channel];
      random.nextBytes(bbytes);
      bMat.data().put(bbytes);

      Mat result = opencv_core.add(aMat, bMat).asMat();
      printMatInfo(result);
    }
  }

  @Test
  public void testMultiply() {

    byte[] fixedMatData = new byte[180 * row];
    random.nextBytes(fixedMatData);
    try (Mat fixedMat = new Mat(180, row, opencv_core.CV_64FC1);
        Mat aMat = new Mat(row, col, opencv_core.CV_64FC(channel))) {
      fixedMat.data().put(fixedMatData);
      byte[] abytes = new byte[row * col * channel];
      random.nextBytes(abytes);
      aMat.data().put(abytes);

      Assertions.assertThrows(
          RuntimeException.class, () -> opencv_core.multiply(fixedMat, aMat).asMat());
    }
  }

  @Test
  public void testMultiply2() {

    byte[] fixedMatData = new byte[180 * row];
    random.nextBytes(fixedMatData);
    try (Mat fixedMat = new Mat(180, row, opencv_core.CV_64FC1);
        Mat aMat = new Mat(row, col, opencv_core.CV_64FC(channel));
        Mat bMat = new Mat(180, col, opencv_core.CV_64FC(channel))) {
      fixedMat.data().put(fixedMatData);
      byte[] abytes = new byte[row * col * channel];
      random.nextBytes(abytes);
      aMat.data().put(abytes);

      MatVector resultVector = new MatVector();
      MatVector matVector = new MatVector(aMat.channels());
      opencv_core.split(aMat, matVector);

      printMatInfo(bMat);
      for (int i = 0; i < aMat.channels(); ++i) {
        Mat mat = opencv_core.multiply(fixedMat, matVector.get(i)).asMat();
        printMatInfo(mat);
        resultVector.push_back(mat);
      }

      opencv_core.merge(resultVector, bMat);
      printMatInfo(bMat);
    }
  }
}
