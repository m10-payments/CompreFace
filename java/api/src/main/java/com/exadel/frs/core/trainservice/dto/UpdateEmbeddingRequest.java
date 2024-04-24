package com.exadel.frs.core.trainservice.dto;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateEmbeddingRequest {
    Map<String, String> imageAttributes;
}
