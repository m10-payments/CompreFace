package com.exadel.frs.core.trainservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true) // here and below "ignoreUnknown = true" for backward compatibility
public class CacheActionDto<T> {
    CacheAction cacheAction;
    String apiKey;
    @JsonProperty("uuid")
    UUID serverUUID;
    T payload;

    public <S> CacheActionDto<S> withPayload(S newPayload) {
        return new CacheActionDto<>(
            cacheAction,
            apiKey,
            serverUUID,
            newPayload
        );
    }

    public enum CacheAction {
        // UPDATE and DELETE stays here to support rolling update
        @Deprecated
        UPDATE,
        @Deprecated
        DELETE,
        REMOVE_EMBEDDINGS,
        REMOVE_SUBJECTS,
        ADD_EMBEDDINGS,
        RENAME_SUBJECTS,
        INVALIDATE
    }

    @Value
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemoveEmbeddings {
        Map<String, List<UUID>> embeddings;
    }

    @Value
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemoveSubjects {
        List<String> subjects;
    }

    @Value
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddEmbeddings {
        List<UUID> embeddings;
    }

    @Value
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RenameSubjects {
        Map<String, String> subjectsNamesMapping;
    }
}
