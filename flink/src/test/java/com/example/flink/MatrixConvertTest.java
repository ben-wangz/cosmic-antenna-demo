package com.example.flink;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;

public class MatrixConvertTest {

  private static final byte[] array =
      new byte[] {
        -46, 58, 68, -85, -16, 9, -21, 60, -59, 5, -8, 93, -72, 80, 35, 39, 7, 10, -109, 120, 12,
        59, 84, -104, 14, 76, 68, 27, -47, 87, 40, -64, 114, -81, 16, 123, -56, -13, -37, 127, 48,
        101
      };

  @Test
  public void endiannessCheck() {
    ByteBuffer buffer = ByteBuffer.wrap(array);
    buffer.order(ByteOrder.BIG_ENDIAN);
  }

  @Test
  public void convert() {
    Mat mat = imdecode(new Mat(array), IMREAD_GRAYSCALE);
    mat.create(6, 7, CV_8UC1);
    printMat(mat);
  }

  @Test
  public void insert() {
    try (Mat realMat = new Mat(6, 7, CV_8UC1)) {
      Indexer realMatIndexer = realMat.createIndexer();
      for (int i = 0; i < 6; i++) {
        for (int j = 0; j < 7; j++) {
          long[] position = new long[] {i, j};
          realMatIndexer.putDouble(position, array[i * 6 + j]);
        }
      }
      printMat(realMat);
    }
  }

  @Test
  public void build() {
    Mat matrix2D = new Mat(6, 7, CvType.CV_8UC1);
    matrix2D.data().put(array);
    printMat(matrix2D);
  }

  private static void printMat(Mat mat) {
    int numRows = mat.rows();
    int numCols = mat.cols();
    ByteBuffer buffer = mat.createBuffer();
    for (int i = 0; i < numRows; i++) {
      for (int j = 0; j < numCols; j++) {
        System.out.print(buffer.get() + " ");
      }
      System.out.println();
    }
  }
}
