package com.example.flink;

import static org.bytedeco.opencv.global.opencv_core.CV_64FC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_UNCHANGED;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;

import java.util.Arrays;

import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;

public class MatrixConvertTest {
    private static final byte[] array = new byte[]{-46, 58, 68, -85, -16, 9, -21, 60, -59, 5, -8, 93, -72,
            80, 35, 39, 7, 10, -109, 120, 12, 59, 84, -104, 14, 76, 68, 27, -47, 87,
            40, -64, 114, -81, 16, 123, -56, -13, -37, 127, 48, 101};
    @Test
    public void convert() {
        Mat mat = imdecode(new Mat(array), IMREAD_UNCHANGED);
        mat.create(6, 7, opencv_core.CV_8UC(1));
        System.out.println(mat.empty());
        byte[] result = new byte[42];
        mat.data().get(result);
        mat.release();
        System.out.println(Arrays.toString(result));
    }

    @Test
    public void insert(){
        try (Mat realMat = new Mat(6, 7, CV_64FC1)) {
            Indexer realMatIndexer = realMat.createIndexer();
            for (int i = 0; i < 6; i++){
                for (int j = 0; j < 7; j++){
                    long[] position = new long[]{i, j};
                    realMatIndexer.putDouble(position, array[i * 6 + j]);
                }
            }
            byte[] result = new byte[42];
            realMat.data().get(result);
            realMat.release();
            System.out.println(Arrays.toString(result));
        }
    }
}
