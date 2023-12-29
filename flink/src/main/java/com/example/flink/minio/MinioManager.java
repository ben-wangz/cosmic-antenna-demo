package com.example.flink.minio;

import com.google.common.base.Preconditions;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinioManager {
  public static final Logger LOGGER = LoggerFactory.getLogger(MinioManager.class);
  private final MinioClient minioClient;

  @Builder
  public MinioManager(MinioConnection minioConnection) {
    String endpoint = minioConnection.getEndpoint();
    String accessKey = minioConnection.getAccessKey();
    String accessSecret = minioConnection.getAccessSecret();
    Preconditions.checkArgument(
        StringUtils.isNotBlank(endpoint), "endpoint(%s) cannot be blank", endpoint);
    Preconditions.checkArgument(
        StringUtils.isNotBlank(endpoint), "accessKey(%s) cannot be blank", endpoint);
    Preconditions.checkArgument(
        StringUtils.isNotBlank(endpoint), "accessSecret(%s) cannot be blank", endpoint);
    minioClient =
        MinioClient.builder().endpoint(endpoint).credentials(accessKey, accessSecret).build();
  }

  public void objectUpload(
      InputStream inputStream, String bucketName, String objectKey, Map<String, String> tags)
      throws MinioException {
    try {
      minioClient.putObject(
          PutObjectArgs.builder().tags(tags).bucket(bucketName).object(objectKey).stream(
                  inputStream, -1, 10485760)
              .build());
    } catch (ServerException
        | InsufficientDataException
        | ErrorResponseException
        | IOException
        | NoSuchAlgorithmException
        | InvalidKeyException
        | InvalidResponseException
        | XmlParserException
        | InternalException e) {
      throw new MinioException(e);
    }
  }

  public void objectUpload(InputStream inputStream, String bucketName, String objectKey)
      throws MinioException {
    objectUpload(inputStream, bucketName, objectKey, Map.of());
  }
}
