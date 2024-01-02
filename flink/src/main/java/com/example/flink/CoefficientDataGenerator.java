package com.example.flink;

import com.example.flink.data.CoefficientData;
import com.example.flink.minio.MinioConnection;
import com.example.flink.minio.MinioManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoefficientDataGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(CosmicAntennaApp.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static final String FILE_PREFIX = "coefficient-";

  public static void main(String[] args) throws IOException {
    Configuration configuration = CosmicAntennaConf.ConfigurationBuilder.build();
    Integer channelSize = configuration.get(CosmicAntennaConf.CHANNEL_SIZE);

    Path generatedDirPath = generateCoefficientDataFile(channelSize);
    String endpoint =
        Optional.ofNullable(System.getenv("S3_ENDPOINT")).orElse("http://10.11.33.132:9000");
    String accessKey = Optional.ofNullable(System.getenv("S3_ACCESS_KEY")).orElse("minioadmin");
    String secretKey = Optional.ofNullable(System.getenv("S3_SECRET_KEY")).orElse("minioadmin");
    String bucket = Optional.ofNullable(System.getenv("FILE_SAVE_BUCKET")).orElse("flink");

    MinioManager minioManager =
        MinioManager.builder()
            .minioConnection(
                MinioConnection.builder()
                    .endpoint(endpoint)
                    .accessKey(accessKey)
                    .accessSecret(secretKey)
                    .build())
            .build();

    String baseName = generatedDirPath.getFileName().toString();
    Arrays.stream(Objects.requireNonNull(generatedDirPath.toFile().listFiles()))
        .forEach(
            file -> {
              try (FileInputStream fileInputStream = new FileInputStream(file)) {
                minioManager.objectUpload(fileInputStream, bucket, baseName + "/" + file.getName());
              } catch (Exception e) {
                throw new RuntimeException("upload file failed, since ", e);
              }
            });

    System.out.printf(
        "you also need to execute command \n \"set %s=%s\"%n",
        CosmicAntennaConf.COEFFICIENT_DATA_PATH.key(), baseName);
    System.exit(0);
  }

  public static Path generateCoefficientDataFile(Integer channelSize) throws IOException {
    Path tempDirectory = Files.createTempDirectory("coefficient-matrix-" + channelSize + "-");

    LOGGER.debug("coefficient data saved in -> {}", tempDirectory);
    Random random = new Random(666);

    IntStream.range(0, channelSize)
        .boxed()
        .map(
            channelId -> {
              byte[] realArray = new byte[224 * 180];
              byte[] imaginaryArray = new byte[224 * 180];
              random.nextBytes(realArray);
              random.nextBytes(imaginaryArray);
              return CoefficientData.builder()
                  .channelId(channelId)
                  .realArray(realArray)
                  .imaginaryArray(imaginaryArray)
                  .build();
            })
        .forEach(
            item -> {
              try {
                Path tempFile =
                    Files.createTempFile(
                        tempDirectory, FILE_PREFIX + item.getChannelId() + "-", ".json");
                FileUtils.writeFileUtf8(tempFile.toFile(), OBJECT_MAPPER.writeValueAsString(item));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });

    return tempDirectory;
  }
}
