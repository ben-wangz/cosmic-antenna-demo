package com.example.flink.data;

import java.io.Serializable;
import java.util.Set;
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
public class ChannelDataACC implements Serializable {
  private static final long serialVersionUID = 3791844431556452626L;
  private Set<Integer> gatheredAntennaIdSet;
  private ChannelData channelData;

  @Jacksonized
  @Builder(toBuilder = true)
  public ChannelDataACC(
      @NonNull Set<Integer> gatheredAntennaIdSet, @NonNull ChannelData channelData) {
    this.gatheredAntennaIdSet = gatheredAntennaIdSet;
    this.channelData = channelData;
  }
}
