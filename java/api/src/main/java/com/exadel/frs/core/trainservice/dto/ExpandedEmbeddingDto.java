package com.exadel.frs.core.trainservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpandedEmbeddingDto {
    // As of backward compatibility we are not allowed to rename property 'image_id' --> 'embedding_id'
    // but, notice, actually it is Embedding.id
    @JsonProperty("image_id")
    private String embeddingId;

    @JsonProperty("subject")
    private String subjectName;

    @JsonProperty("image_attributes")
    private Map<String, String> imageAttributes;
}
