package com.example.opencv;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.CV_64FC1;
import static org.bytedeco.opencv.global.opencv_core.randu;

import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;

public class Main {
    public static void main(String[] args) {
        Mat randomMat = new Mat(224, 256, CV_64FC1);
        Indexer indexer = randomMat.createIndexer();
        System.out.println(indexer.toString());
        double low = -500.0;
        double high = 500.0;
        randu(randomMat,
                new Mat(1, 1, CV_32F, new Scalar(low)),
                new Mat(1, 1, CV_32F, new Scalar(high)));
        System.out.println(indexer.toString());
    }
}
