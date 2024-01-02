package com.example.flink.minio;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@EqualsAndHashCode
@ToString
public class MinioFile {

  private final String objectKey;
  private final boolean isDirectory;
  private final long size;

  @Builder(toBuilder = true)
  @Jacksonized
  public MinioFile(String objectKey, boolean isDirectory, long size) {
    this.objectKey = objectKey;
    this.isDirectory = isDirectory;
    this.size = size;
  }
}
