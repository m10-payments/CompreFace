package com.exadel.frs.core.trainservice.dto;

import java.util.Map;
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

    @NotNull
    Map<String, String> imageAttributes;
}
