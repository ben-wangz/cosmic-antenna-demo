package com.example.s3sync;

import com.example.s3sync.minio.MinioConnection;
import com.example.s3sync.minio.MinioFile;
import com.example.s3sync.minio.MinioManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode
@ToString
public class SyncS3Object {
  private static final Logger LOGGER = LoggerFactory.getLogger(SyncS3Object.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String uniqueInstanceName;
  private final String bucket;
  private final String objectKey;
  private final File targetPath;
  private final MinioConnection minioConnection;
  private final long timeoutMilliseconds;
  private final Integer maxWaitAttempts;
  private final Integer retrySleepSeconds;
  private transient MinioManager minioManager;

  @Builder
  @Jacksonized
  public SyncS3Object(
      @NotNull String uniqueInstanceName,
      @NotNull String bucket,
      @NotNull String objectKey,
      @NotNull File targetPath,
      @NotNull MinioConnection minioConnection,
      @NotNull Long timeoutMilliseconds,
      Integer maxWaitAttempts,
      Integer retrySleepSeconds) {
    this.uniqueInstanceName = uniqueInstanceName;
    this.bucket = bucket;
    this.objectKey = StringUtils.removeStart(objectKey, "/");
    this.targetPath = targetPath;
    this.minioConnection = minioConnection;
    this.timeoutMilliseconds = timeoutMilliseconds;
    this.maxWaitAttempts = null == maxWaitAttempts ? 10 : maxWaitAttempts;
    this.retrySleepSeconds = null == retrySleepSeconds ? 1 : retrySleepSeconds;
  }

  public void sync() throws IOException {
    connect();
    if (!minioManager.isDirectoryPath(bucket, objectKey)) {
      syncFile(objectKey, targetPath);
      return;
    }
    for (MinioFile file : minioManager.objectList(bucket, objectKey, true)) {
      syncFile(
          file.getObjectKey(),
          FileUtils.getFile(targetPath, StringUtils.removeStart(file.getObjectKey(), objectKey)));
    }
  }

  private void syncFile(String objectKey, File targetFile) throws IOException {
    File lockFile = FileUtils.getFile(targetFile.getAbsolutePath() + ".lock");
    boolean lockedByUs = seizeLock(targetFile);
    if (lockedByUs) {
      File tempFile =
          FileUtils.getFile(
              FilenameUtils.getFullPath(targetFile.getAbsolutePath()),
              String.format(
                  "%s.%s.tmp",
                  FilenameUtils.getBaseName(targetFile.getName()), uniqueInstanceName));
      try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
        InputStream inputStream =
            minioManager.objectGetFromOffset(bucket, objectKey, tempFile.length());
        IOUtils.copy(inputStream, fileOutputStream);
        fileOutputStream.flush();
      } finally {
        FileUtils.moveFile(tempFile, targetFile);
        FileUtils.deleteQuietly(lockFile);
      }
    }
  }

  // TODO may not be able to atomically create the lock file
  private boolean seizeLock(File savedPath) throws IOException {
    File lockFile = FileUtils.getFile(savedPath.getAbsolutePath() + ".lock");
    for (int index = 0; index < maxWaitAttempts; index++) {
      if (syncSuccess(savedPath)) {
        return false;
      }
      if (!lockFile.exists()) {
        createLockFile(lockFile);
        return true;
      }
      LockFileContent lockFileContent =
          OBJECT_MAPPER.readValue(
              FileUtils.readFileToString(lockFile, StandardCharsets.UTF_8),
              new TypeReference<>() {});
      if (StringUtils.equals(uniqueInstanceName, lockFileContent.getUniqueInstanceName())) {
        return true;
      }
      if (lockFileContent.getCreated() + timeoutMilliseconds > System.currentTimeMillis()) {
        FileUtils.deleteQuietly(lockFile);
        createLockFile(lockFile);
        return true;
      }
      try {
        TimeUnit.SECONDS.sleep(retrySleepSeconds);
      } catch (InterruptedException e) {
        LOGGER.warn(String.format("sleep interrupted, since %s", e.getMessage()), e);
      }
    }
    return false;
  }

  private void createLockFile(File lockFile) {
    try {
      String lockContent =
          OBJECT_MAPPER.writeValueAsString(
              LockFileContent.builder()
                  .uniqueInstanceName(uniqueInstanceName)
                  .created(System.currentTimeMillis())
                  .build());
      FileUtils.writeStringToFile(lockFile, lockContent, StandardCharsets.UTF_8, false);
      LOGGER.info("created a new lock: {}", lockContent);
    } catch (IOException e) {
      if (lockFile.exists()) {
        FileUtils.deleteQuietly(lockFile);
      }
      throw new UncheckedIOException(
          String.format("create lock file failed, since %s", e.getMessage()), e);
    }
  }

  private void connect() {
    if (null == minioManager) {
      minioManager = MinioManager.builder().minioConnection(minioConnection).build();
    }
  }

  private boolean syncSuccess(File targetFile) {
    return targetFile.exists();
  }
}
