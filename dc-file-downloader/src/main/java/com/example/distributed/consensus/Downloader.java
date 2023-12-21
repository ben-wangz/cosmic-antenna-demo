package com.example.distributed.consensus;

import com.example.distributed.consensus.utils.ParameterUtil;

public class Downloader {
    public static void main(String[] args) {
        String endpoint = ParameterUtil.readEnvironmentVariable("S3_ENDPOINT", String.class);
        String accessKey = ParameterUtil.readEnvironmentVariable("S3_ACCESS_KEY", String.class);
        String secretKey = ParameterUtil.readEnvironmentVariable("S3_SECRET_KEY", String.class);
        String bucket = ParameterUtil.readEnvironmentVariable("DC_FILE_BUCKET", String.class);
        String filePath = ParameterUtil.readEnvironmentVariable("DC_FILE_PATH", String.class);


        System.out.println("Hello world!");
    }
}