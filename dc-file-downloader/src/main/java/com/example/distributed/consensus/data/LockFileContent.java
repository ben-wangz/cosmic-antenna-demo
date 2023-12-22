package com.example.distributed.consensus.data;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class LockFileContent {

    private String hostName;

    private Long created;

    @Builder(toBuilder = true)
    @Jacksonized
    public LockFileContent(
            @NonNull String hostName,
            @NonNull Long created) {
        this.hostName = hostName;
        this.created = created;
    }
}
