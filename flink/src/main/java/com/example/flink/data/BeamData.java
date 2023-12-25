package com.example.flink.data;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class BeamData {

  private Integer channelId;
  private Integer beamId;
  @ToString.Exclude private byte[] resultArray;

  @Builder
  @Jacksonized
  public BeamData(
      @NonNull Integer channelId, @NonNull Integer beamId, byte @NonNull [] resultArray) {
    this.channelId = channelId;
    this.beamId = beamId;
    this.resultArray = resultArray;
  }
}
