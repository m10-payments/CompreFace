package com.exadel.frs.core.trainservice.cache;

import com.exadel.frs.commonservice.entity.Embedding;
import com.exadel.frs.commonservice.entity.EmbeddingProjection;
import com.exadel.frs.commonservice.entity.EnhancedEmbeddingProjection;
import com.exadel.frs.commonservice.exception.IncorrectImageIdException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EmbeddingCollection {

    private final ConcurrentMap<String, Map<UUID, RealVector>> mapping;

    public EmbeddingCollection() {
        this.mapping = new ConcurrentHashMap<>();
    }

    public static EmbeddingCollection from(final Collection<EnhancedEmbeddingProjection> projections) {
        // we copy vector here just in case
        var newMap = projections.stream().map(e -> Map.entry(e.getSubjectName(), Pair.of(e.getEmbeddingId(), MatrixUtils.createRealVector(e.getEmbeddingData()))))
                .collect(
                        Collectors.toConcurrentMap(
                                Entry::getKey,
                                entry -> {
                                    Map<UUID, RealVector> map = new ConcurrentHashMap<>();
                                    map.put(entry.getValue().getFirst(), entry.getValue().getSecond());
                                    return map;
                                },
                                (map1, map2) -> {
                                    map1.putAll(map2);
                                    return map1;
                                }
                        )
                );
        return new EmbeddingCollection(newMap);
    }

    public <T> T visit(Function<Map<String, Map<UUID, RealVector>>, T> readAndDo) {
        return readAndDo.apply(exposeMap());
    }

    // package private for test purposes
    Map<String, Map<UUID, RealVector>> exposeMap() {
        return Collections.unmodifiableMap(mapping);
    }

    public void updateSubjectName(String oldSubjectName, String newSubjectName) {
        mapping.put(newSubjectName, mapping.remove(oldSubjectName));
    }

    public EmbeddingProjection addEmbedding(final Embedding embedding) {
        var id = embedding.getId();
        var realVector = MatrixUtils.createRealVector(embedding.getEmbedding());
        mapping.computeIfAbsent(embedding.getSubject().getSubjectName(), k -> new ConcurrentHashMap<>())
                .put(id, realVector);
        return new EmbeddingProjection(id, embedding.getSubject().getSubjectName());
    }

    public void addEmbedding(final EnhancedEmbeddingProjection projection) {
        var res = mapping.computeIfAbsent(projection.getSubjectName(), k -> new ConcurrentHashMap<>())
                .put(projection.getEmbeddingId(), MatrixUtils.createRealVector(projection.getEmbeddingData()));
        if (res != null) {
            log.error("Embedding with id {} already exists", projection.getEmbeddingId());
        }
    }

    public void removeEmbeddingsBySubjectName(String subjectName) {
        mapping.remove(subjectName);
    }

    public EmbeddingProjection removeEmbedding(EmbeddingProjection projection) {
        var wasRemoved = new AtomicBoolean(false);
        mapping.compute(
                projection.getSubjectName(),
                (k, v) -> {
                    if (v == null) {
                        return null;
                    }
                    if (v.remove(projection.getEmbeddingId()) != null) {
                        wasRemoved.set(true);
                        if (v.isEmpty()) {
                            return null;
                        }
                    }
                    return v;
                });
        return wasRemoved.get() ? projection : null;
    }

    public Optional<double[]> getRawEmbeddingById(UUID embeddingId) {
        return findByEmbeddingId(
                embeddingId,
                // return duplicated row
                entry -> entry.getValue().getValue().toArray()
        );
    }

    public Optional<String> getSubjectNameByEmbeddingId(UUID embeddingId) {
        return findByEmbeddingId(
                embeddingId,
                Entry::getKey
        );
    }

    public Optional<Map<UUID, RealVector>> getEmbeddingsBySubjectName(@NonNull String subjectName) {
        return Optional.ofNullable(mapping.get(subjectName));
    }

    private <T> Optional<T> findByEmbeddingId(UUID embeddingId, Function<Map.Entry<String, Map.Entry<UUID, RealVector>>, T> func) {
        validImageId(embeddingId);
        return mapping.entrySet().stream()
                .filter(entry -> entry.getValue().containsKey(embeddingId))
                .findFirst()
                .map(entry -> Map.entry(entry.getKey(), Map.entry(embeddingId, entry.getValue().get(embeddingId))))
                .map(func);
    }

    private void validImageId(UUID embeddingId) {
        if (embeddingId == null) {
            throw new IncorrectImageIdException();
        }
    }
}
