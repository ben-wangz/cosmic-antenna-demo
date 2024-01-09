package com.example.s3sync;

import com.example.s3sync.minio.MinioConnection;
import java.io.File;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
  private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
  private static String bucket;
  private static String hostName;

  public static void main(String[] args) throws Exception {
    String endpoint =
        Optional.ofNullable(System.getenv("ENDPOINT")).orElse("http://api-minio-dev.lab.zjvis.net:32080");
    String accessKey = Optional.ofNullable(System.getenv("ACCESS_KEY")).orElse("conti");
    String accessSecret = Optional.ofNullable(System.getenv("ACCESS_SECRET")).orElse("Conti@1234");
    bucket =
        Optional.ofNullable(System.getenv("BUCKET"))
            .orElseThrow(() -> new Exception("BUCKET is required"));
    String objectKey =
        Optional.ofNullable(System.getenv("OBJECT_KEY"))
            .orElseThrow(() -> new Exception("OBJECT_KEY is required"));
    String targetPath =
        Optional.ofNullable(System.getenv("TARGET_PATH"))
            .orElseThrow(() -> new Exception("TARGET_PATH is required"));
    Long timeoutMilliseconds =
        Optional.ofNullable(System.getenv("TIMEOUT_MILLISECONDS"))
            .map(Long::parseLong)
            .orElse(60 * 1000L);
    hostName = Optional.ofNullable(System.getenv("HOSTNAME")).orElse("localhost");
    LOGGER.info("hostName:{}", hostName);
    LOGGER.info(
        "sync source file from {}@{}/{}/{} to {}",
        accessKey,
        endpoint,
        bucket,
        objectKey,
        targetPath);
    SyncS3Object syncS3Object =
        SyncS3Object.builder()
            .uniqueInstanceName(hostName)
            .bucket(bucket)
            .objectKey(objectKey)
            .targetPath(new File(targetPath))
            .minioConnection(
                MinioConnection.builder()
                    .endpoint(endpoint)
                    .accessKey(accessKey)
                    .accessSecret(accessSecret)
                    .build())
            .timeoutMilliseconds(timeoutMilliseconds)
            .build();
    syncS3Object.sync();
    System.exit(0);
  }
}
