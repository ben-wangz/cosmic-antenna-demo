package com.example.s3sync;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@EqualsAndHashCode
@ToString
public class LockFileContent {
  private String uniqueInstanceName;
  private Long created;

  @Builder(toBuilder = true)
  @Jacksonized
  public LockFileContent(@NonNull String uniqueInstanceName, @NonNull Long created) {
    this.uniqueInstanceName = uniqueInstanceName;
    this.created = created;
  }
}
