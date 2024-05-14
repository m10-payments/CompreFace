package com.exadel.frs.core.trainservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true) // here and below "ignoreUnknown = true" for backward compatibility
public record CacheActionDto<T>(
    CacheAction cacheAction,
    String apiKey,
    @JsonProperty("uuid")
    UUID serverUUID,
    T payload
) {
    public <S> CacheActionDto<S> withPayload(S payload) {
        return new CacheActionDto<>(
            cacheAction,
            apiKey,
            serverUUID,
            payload
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RemoveEmbeddings(
        Map<String, List<UUID>> embeddings
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RemoveSubjects(
        List<String> subjects
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddEmbeddings(
        List<UUID> embeddings
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RenameSubjects(
        Map<String, String> subjectsNamesMapping
    ) {
    }
}
