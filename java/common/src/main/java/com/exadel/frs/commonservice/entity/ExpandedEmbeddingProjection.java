package com.exadel.frs.commonservice.entity;

import java.util.Map;
import java.util.UUID;
import lombok.Value;
import org.springframework.data.annotation.PersistenceConstructor;


@Value
public class ExpandedEmbeddingProjection {
    UUID embeddingId;
    String subjectName;
    Map<String, String> imageAttributes;

    @PersistenceConstructor
    @SuppressWarnings("unchecked")
    public ExpandedEmbeddingProjection(UUID embeddingId, String subjectName, Object imageAttributes) {
        this.embeddingId = embeddingId;
        this.subjectName = subjectName;
        this.imageAttributes = (Map<String, String>) imageAttributes;
    }
}
