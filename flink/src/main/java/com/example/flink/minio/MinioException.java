package com.example.flink.minio;

import java.io.IOException;

public class MinioException extends IOException {
  private static final long serialVersionUID = 7890718407491122229L;

  public MinioException(String message) {
    super(message);
  }

  public MinioException(String message, Throwable cause) {
    super(message, cause);
  }

  public MinioException(Throwable cause) {
    super(cause);
  }
}
