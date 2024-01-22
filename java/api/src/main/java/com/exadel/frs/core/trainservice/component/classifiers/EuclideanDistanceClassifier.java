/*
 * Copyright (c) 2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.exadel.frs.core.trainservice.component.classifiers;

import com.exadel.frs.commonservice.exception.IncorrectImageIdException;
import com.exadel.frs.commonservice.sdk.faces.FacesApiClient;
import com.exadel.frs.commonservice.sdk.faces.exception.FacesServiceException;
import com.exadel.frs.commonservice.sdk.faces.feign.dto.FacesStatusResponse;
import com.exadel.frs.core.trainservice.cache.EmbeddingCacheProvider;
import com.google.common.primitives.Doubles;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.FastMath;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EuclideanDistanceClassifier implements Classifier {

    public static final int PREDICTION_COUNT_INFINITY = -1;
    private final EmbeddingCacheProvider embeddingCacheProvider;
    private final FacesApiClient facesApiClient;

    @Override
    public List<Pair<Double, String>> predict(final double[] input, final String apiKey, final int resultCount) {
        var inputFace = MatrixUtils.createRealVector(input).unitVector();
        var coefficients = getSimilarityCoefficients();
        return embeddingCacheProvider.getOrLoad(apiKey)
                .visit(
                        ec -> ec.entrySet()
                                .stream()
                                .flatMap(e -> e.getValue().values().stream().map(v -> Pair.of(e.getKey(), v)))
                                .map(e -> Pair.of(calculateSimilarity(e.getValue().getDistance(inputFace), coefficients), e.getKey()))
                                .sorted((p1, p2) -> -Doubles.compare(p1.getLeft(), p2.getLeft()))
                                .limit(resultCount == PREDICTION_COUNT_INFINITY ? Long.MAX_VALUE : resultCount)
                                .collect(Collectors.toList())
                );
    }

    @Override
    public double[] verify(double[] sourceImageEmbedding, double[][] targetImageEmbedding) {
        var sourceNormalized = MatrixUtils.createRealVector(sourceImageEmbedding).unitVector();
        var targetNormalized = normalizeRowsInMatrix(targetImageEmbedding);

        return recognize(sourceNormalized, targetNormalized);
    }

    @Override
    public Double verify(final double[] input, final String apiKey, final UUID embeddingId) {
        if (input == null) {
            return (double) 0;
        }

        var embedding = embeddingCacheProvider.getOrLoad(apiKey)
                .getRawEmbeddingById(embeddingId)
                .orElseThrow(IncorrectImageIdException::new);

        var probabilities = recognize(
                MatrixUtils.createRealVector(input).unitVector(),
                MatrixUtils.createRealMatrix(new double[][]{embedding})
        );

        return probabilities[0];
    }

    @NonNull
    public List<Pair<UUID, Double>> verifySubject(
            @NonNull final String apiKey,
            @NonNull final double[] input,
            @NonNull final String subjectName,
            final int resultCount
    ) {
        final var normalizedEmbedding = MatrixUtils.createRealVector(input).unitVector();
        return embeddingCacheProvider.getEmbeddings(apiKey, subjectName)
                .map(embeddings -> recognize(normalizedEmbedding, embeddings, resultCount))
                .orElse(List.of());
    }

    public double[] normalizeOne(double[] input) {
        return new ArrayRealVector(input).unitVector().toArray();
    }

    /**
     * (tanh ((coef0 - distance) * coef1) + 1) / 2
     * Package protected for testing purposes
     */
    double calculateSimilarity(Double value, SimilarityCoefficients coefficients) {
        return (FastMath.tanh((coefficients.getCoefficient0() - value) * coefficients.getCoefficient1()) + 1) / 2;
    }

    private RealMatrix normalizeRowsInMatrix(final double[][] input) {
        var resultMatrix = MatrixUtils.createRealMatrix(input.length, input[0].length);
        for (int i = 0; i < input.length; i++) {
            var vector = MatrixUtils.createRealVector(input[i]).unitVector();
            resultMatrix.setRowVector(i, vector);
        }
        return resultMatrix;
    }

    private double[] recognize(final RealVector newFace, final RealMatrix existingFaces) {
        var coefficients = getSimilarityCoefficients();
        return Arrays.stream(euclideanDistance(newFace, existingFaces))
                .map(d -> calculateSimilarity(d, coefficients))
                .toArray();
    }

    private SimilarityCoefficients getSimilarityCoefficients() {
        FacesStatusResponse status = facesApiClient.getStatus();
        return Optional.ofNullable(status)
                .map(FacesStatusResponse::getSimilarityCoefficients)
                .filter(cfs -> cfs.size() >= 2)
                .map(cfs -> new SimilarityCoefficients(cfs.get(0), cfs.get(1)))
                .orElseThrow(() -> new FacesServiceException("No status information received"));
    }

    private List<Pair<UUID, Double>> recognize(RealVector normalizedEmbedding, Map<UUID, RealVector> embeddings, int resultCount) {
        var coefficients = getSimilarityCoefficients();
        return embeddings.entrySet()
                .stream()
                .map(e -> Pair.of(e.getKey(), calculateSimilarity(e.getValue().getDistance(normalizedEmbedding), coefficients)))
                .sorted((p1, p2) -> -Doubles.compare(p1.getRight(), p2.getRight()))
                .limit(resultCount)
                .collect(Collectors.toList());
    }

    private static double[] euclideanDistance(final RealVector newFace, RealMatrix existingFaces) {
        int numRows = existingFaces.getRowDimension();
        // Create an array to hold the distances
        double[] distances = new double[numRows];
        // Calculate the Euclidean distance between each row and the vector
        for (int i = 0; i < numRows; i++) {
            var rowVector = existingFaces.getRowVector(i);
            distances[i] = rowVector.getDistance(newFace);
        }
        return distances;
    }

    @Getter
    @RequiredArgsConstructor
    static final class SimilarityCoefficients {
        private final double coefficient0;
        private final double coefficient1;
    }
}