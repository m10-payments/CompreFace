package com.exadel.frs.core.trainservice.dto;

import javax.validation.constraints.NotNull;
import lombok.Value;
import org.springframework.lang.Nullable;

@Value
public class EmbeddingInfo {

    @NotNull
    String calculator;

    @NotNull
    double[] embedding;

    @Nullable
    byte[] source;
}
