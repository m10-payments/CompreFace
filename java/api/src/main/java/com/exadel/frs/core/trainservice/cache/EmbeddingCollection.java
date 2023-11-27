package com.exadel.frs.core.trainservice.cache;

import com.exadel.frs.commonservice.entity.Embedding;
import com.exadel.frs.commonservice.entity.EmbeddingProjection;
import com.exadel.frs.commonservice.entity.EnhancedEmbeddingProjection;
import com.exadel.frs.commonservice.exception.IncorrectImageIdException;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmbeddingCollection {

    private final BiMap<EmbeddingProjection, Integer> projection2Index;
    private INDArray embeddings;
    private ReadLock readLock;
    private WriteLock writeLock;

    public static EmbeddingCollection from(final Stream<EnhancedEmbeddingProjection> stream) {
        val rawEmbeddings = new LinkedList<double[]>();
        val projections2Index = new HashMap<EmbeddingProjection, Integer>();
        val index = new AtomicInteger(); // just to bypass 'final' variables restriction inside lambdas

        stream.forEach(projection -> {
            projections2Index.put(EmbeddingProjection.from(projection), index.getAndIncrement());
            rawEmbeddings.add(projection.getEmbeddingData());
        });
        var reentrantLock = new ReentrantReadWriteLock();
        return new EmbeddingCollection(
                HashBiMap.create(projections2Index),
                rawEmbeddings.isEmpty()
                        ? Nd4j.empty()
                        : Nd4j.create(rawEmbeddings.toArray(double[][]::new)),
                reentrantLock.readLock(),
                reentrantLock.writeLock()
        );
    }

    // TODO: provide immutable wrapper for EmbeddingCollection here
    public <T> T doWithReadLock(Function<EmbeddingCollection, T> readAndDo) {
        readLock.lock();
        try {
            return readAndDo.apply(this);
        } finally {
            readLock.unlock();
        }
    }

    // NB: should be used only with read lock
    public Map<Integer, EmbeddingProjection> getIndexMap() {
        // returns index to projection map
        return Collections.unmodifiableMap(projection2Index.inverse());
    }

    // package private for test purposes
    Set<EmbeddingProjection> getProjections() {
        // returns index to projection map
        return Collections.unmodifiableSet(projection2Index.keySet());
    }

    private int getSize() {
        // should be invoked only if underlying array is not empty!
        return (int) embeddings.size(0);
    }

    // NB: should be used only with read lock or in tests
    public INDArray getEmbeddings() {
        return embeddings;
    }

    public void updateSubjectName(String oldSubjectName, String newSubjectName) {
        writeLock.lock();
        try {
            final List<EmbeddingProjection> projections = projection2Index.keySet()
                    .stream()
                    .filter(projection -> projection.getSubjectName().equals(oldSubjectName))
                    .collect(Collectors.toList());

            projections.forEach(projection -> projection2Index.put(
                    projection.withNewSubjectName(newSubjectName),
                    projection2Index.remove(projection)
            ));
        } finally {
            writeLock.unlock();
        }
    }

    public EmbeddingProjection addEmbedding(final Embedding embedding) {
        writeLock.lock();
        try {
            final var projection = EmbeddingProjection.from(embedding);

            final INDArray array = Nd4j.create(new double[][]{embedding.getEmbedding()});

            embeddings = embeddings.isEmpty()
                    ? array
                    : Nd4j.concat(0, embeddings, array);

            projection2Index.put(
                    projection,
                    getSize() - 1
            );

            return projection;
        } finally {
            writeLock.unlock();
        }
    }

    public Collection<EmbeddingProjection> removeEmbeddingsBySubjectName(String subjectName) {
        writeLock.lock();
        try {
            // not efficient at ALL! review current approach!

            final List<EmbeddingProjection> toRemove = projection2Index.keySet().stream()
                    .filter(projection -> projection.getSubjectName().equals(subjectName))
                    .collect(Collectors.toList());

            toRemove.forEach(this::removeEmbedding); // <- rethink

            return toRemove;
        } finally {
            writeLock.unlock();
        }
    }

    public EmbeddingProjection removeEmbedding(EmbeddingProjection projection) {
        writeLock.lock();
        try {
            if (projection2Index.isEmpty()) {
                return null;
            }

            var index = projection2Index.remove(projection);

            // remove embedding by concatenating sub lists [0, index) + [index + 1, size),
            // thus size of resulting array is decreased by one
            embeddings = Nd4j.concat(
                    0,
                    embeddings.get(NDArrayIndex.interval(0, index), NDArrayIndex.all()),
                    embeddings.get(NDArrayIndex.interval(index + 1, getSize()), NDArrayIndex.all())
            );

            // shifting (-1) all indexes, greater than current one
            projection2Index.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() > index)
                    .sorted(Map.Entry.comparingByValue())
                    .forEach(e -> projection2Index.replace(e.getKey(), e.getValue(), e.getValue() - 1));

            return projection;
        } finally {
            writeLock.unlock();
        }
    }

    public Optional<INDArray> getRawEmbeddingById(UUID embeddingId) {
        readLock.lock();
        try {
            return findByEmbeddingId(
                    embeddingId,
                    // return duplicated row
                    entry -> embeddings.getRow(entry.getValue(), true).dup()
            );
        } finally {
            readLock.unlock();
        }
    }

    public Optional<String> getSubjectNameByEmbeddingId(UUID embeddingId) {
        readLock.lock();
        try {
            return findByEmbeddingId(
                    embeddingId,
                    entry -> entry.getKey().getSubjectName()
            );
        } finally {
            readLock.unlock();
        }
    }

    private <T> Optional<T> findByEmbeddingId(UUID embeddingId, Function<Map.Entry<EmbeddingProjection, Integer>, T> func) {
        validImageId(embeddingId);

        return Optional.ofNullable(projection2Index.entrySet()
                .stream()
                .filter(entry -> embeddingId.equals(entry.getKey().getEmbeddingId()))
                .findFirst()
                .map(func)
                .orElseThrow(IncorrectImageIdException::new));
    }

    private void validImageId(UUID embeddingId) {
        if (embeddingId == null) {
            throw new IncorrectImageIdException();
        }
    }
}
