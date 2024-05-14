package com.exadel.frs.core.trainservice.service;

import com.exadel.frs.commonservice.projection.EmbeddingProjection;
import com.exadel.frs.core.trainservice.cache.EmbeddingCacheProvider;
import com.exadel.frs.core.trainservice.dto.CacheActionDto;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.AddEmbeddings;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.RemoveEmbeddings;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.RemoveSubjects;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.RenameSubjects;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationHandler {
    private final EmbeddingCacheProvider cacheProvider;
    private final SubjectService subjectService;

    public void removeEmbeddings(CacheActionDto<RemoveEmbeddings> action) {
        action.payload().embeddings()
            .entrySet()
            .stream()
            .filter(e -> StringUtils.isNotBlank(e.getKey()))
            .filter(e -> Objects.nonNull(e.getValue()))
            .filter(e -> !e.getValue().isEmpty())
            .flatMap(e -> e.getValue().stream().filter(Objects::nonNull).map(id -> new EmbeddingProjection(id, e.getKey())))
            .forEach(
                em -> cacheProvider.expose(
                    action.apiKey(),
                    c -> c.removeEmbedding(em)
                )
            );
    }

    public void removeSubjects(CacheActionDto<RemoveSubjects> action) {
        action.payload().subjects()
            .stream()
            .filter(StringUtils::isNotBlank)
            .forEach(
                s -> cacheProvider.expose(
                    action.apiKey(),
                    c -> c.removeEmbeddingsBySubjectName(s)
                )
            );
    }


    public void addEmbeddings(CacheActionDto<AddEmbeddings> action) {
        var filtered = action.payload().embeddings()
            .stream()
            .filter(Objects::nonNull)
            .toList();
        subjectService.loadEmbeddingsById(filtered)
            .forEach(
                em -> cacheProvider.expose(
                    action.apiKey(),
                    c -> c.addEmbedding(em)
                )
            );
    }

    public void renameSubjects(CacheActionDto<RenameSubjects> action) {
        action.payload().subjectsNamesMapping()
            .entrySet()
            .stream()
            .filter(e -> StringUtils.isNotBlank(e.getKey()))
            .filter(e -> StringUtils.isNotBlank(e.getValue()))
            .forEach(
                e -> cacheProvider.expose(
                    action.apiKey(),
                    c -> c.updateSubjectName(e.getKey(), e.getValue())
                )
            );
    }

    public <T> void invalidate(CacheActionDto<T> action) {
        cacheProvider.expose(
            action.apiKey(),
            e -> cacheProvider.receiveInvalidateCache(action.apiKey())
        );
    }

    /**
     * @param action cacheAction
     * @deprecated in favour more granular cache managing.
     * See {@link CacheActionDto}.
     * Stays here to support rolling update
     */
    @Deprecated(forRemoval = true)
    public <T> void handleDelete(CacheActionDto<T> action) {
        cacheProvider.receiveInvalidateCache(action.apiKey());
    }

    /**
     * @param action cacheAction
     * @deprecated in favour more granular cache managing.
     * See {@link CacheActionDto}.
     * Stays here to support rolling update
     */
    @Deprecated(forRemoval = true)
    public <T> void handleUpdate(CacheActionDto<T> action) {
        cacheProvider.receivePutOnCache(action.apiKey());
    }
}
