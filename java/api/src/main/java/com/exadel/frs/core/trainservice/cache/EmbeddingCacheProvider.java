package com.exadel.frs.core.trainservice.cache;

import com.exadel.frs.commonservice.entity.Embedding;
import com.exadel.frs.commonservice.entity.EmbeddingProjection;
import com.exadel.frs.core.trainservice.dto.CacheActionDto;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.AddEmbeddings;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.CacheAction;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.RemoveEmbeddings;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.RemoveSubjects;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.RenameSubjects;
import com.exadel.frs.core.trainservice.service.EmbeddingService;
import com.exadel.frs.core.trainservice.service.NotificationSenderService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.RealVector;
import org.springframework.stereotype.Component;

import static com.exadel.frs.core.trainservice.system.global.Constants.SERVER_UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmbeddingCacheProvider {

    private static final long CACHE_EXPIRATION = 60 * 60 * 24L;
    private static final long CACHE_MAXIMUM_SIZE = 10;

    private final EmbeddingService embeddingService;

    private final NotificationSenderService notificationSenderService;

    private final Lock lock = new ReentrantLock();

    private static final Cache<String, EmbeddingCollection> cache =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(CACHE_EXPIRATION, TimeUnit.SECONDS)
                    .maximumSize(CACHE_MAXIMUM_SIZE)
                    .build();

    public EmbeddingCollection getOrLoad(final String apiKey) {
        var result = cache.getIfPresent(apiKey);
        if (result == null) {
            try {
                lock.lock();
                result = cache.getIfPresent(apiKey);
                if (result == null) {
                    result = embeddingService.doWithEnhancedEmbeddingProjectionStream(apiKey, EmbeddingCollection::from);
                    cache.put(apiKey, result);
                }
            } finally {
                lock.unlock();
            }
        }
        return result;
    }

    public void removeEmbedding(String apiKey, EmbeddingProjection embedding) {
        getOrLoad(apiKey).removeEmbedding(embedding);
        notifyCacheEvent(
            CacheAction.REMOVE_EMBEDDINGS,
            apiKey,
            new RemoveEmbeddings(Map.of(embedding.getSubjectName(), List.of(embedding.getEmbeddingId())))
        );
    }

    public void updateSubjectName(String apiKey, String oldSubjectName, String newSubjectName) {
        getOrLoad(apiKey).updateSubjectName(oldSubjectName, newSubjectName);
        notifyCacheEvent(CacheAction.RENAME_SUBJECTS, apiKey, new RenameSubjects(Map.of(oldSubjectName, newSubjectName)));
    }

    public void removeBySubjectName(String apiKey, String subjectName) {
        getOrLoad(apiKey).removeEmbeddingsBySubjectName(subjectName);
        notifyCacheEvent(CacheAction.REMOVE_SUBJECTS, apiKey, new RemoveSubjects(List.of(subjectName)));
    }


    public void addEmbedding(String apiKey, Embedding embedding) {
        getOrLoad(apiKey).addEmbedding(embedding);
        notifyCacheEvent(CacheAction.ADD_EMBEDDINGS, apiKey, new AddEmbeddings(List.of(embedding.getId())));
    }

    public Optional<Map<UUID, RealVector>> getEmbeddings(String apiKey, String subjectName) {
        return getOrLoad(apiKey).getEmbeddingsBySubjectName(subjectName);
    }

    /**
     * Method can be used to make changes in cache without sending notification.
     * Use it carefully, because changes you do will not be visible for other compreface-api instances
     *
     * @param apiKey domain
     * @param action what to do with {@link EmbeddingCollection}
     */
    public void exposeIfPresent(String apiKey, Consumer<EmbeddingCollection> action) {
        action.accept(getOrLoad(apiKey));
    }

    public void invalidate(final String apiKey) {
        cache.invalidate(apiKey);
        notifyCacheEvent(CacheAction.INVALIDATE, apiKey, null);
    }

    /**
     * @deprecated
     * See {@link com.exadel.frs.core.trainservice.service.NotificationHandler#handleUpdate(CacheActionDto)}
     */
    @Deprecated(forRemoval = true)
    public void receivePutOnCache(String apiKey) {
        var result = embeddingService.doWithEnhancedEmbeddingProjectionStream(apiKey, EmbeddingCollection::from);
        cache.put(apiKey, result);
    }

    public void receiveInvalidateCache(final String apiKey) {
        cache.invalidate(apiKey);
    }

    private <T> void notifyCacheEvent(CacheAction event, String apiKey, T action) {
        CacheActionDto<T> cacheActionDto = new CacheActionDto<>(event, apiKey, SERVER_UUID, action);
        notificationSenderService.notifyCacheChange(cacheActionDto);
    }
}
