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

package com.exadel.frs.core.trainservice.cache;

import com.exadel.frs.commonservice.entity.EmbeddingProjection;
import com.exadel.frs.commonservice.entity.EnhancedEmbeddingProjection;
import java.util.Arrays;
import java.util.UUID;
import org.apache.commons.math3.linear.MatrixUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.exadel.frs.core.trainservice.ItemsBuilder.makeEmbedding;
import static com.exadel.frs.core.trainservice.ItemsBuilder.makeEnhancedEmbeddingProjection;
import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingCollectionTest {

    private static final String API_KEY = "api_key";

    @Test
    void testRemoveFromEmpty() {
        var embeddingCollection = new EmbeddingCollection();
        final EmbeddingProjection removed = embeddingCollection.removeEmbedding(new EmbeddingProjection(UUID.randomUUID(), "subject_name"));

        assertThat(removed).isNull();
    }

    @Test
    void testAddToEmpty() {
        var embeddingCollection = new EmbeddingCollection();
        Assertions.assertThat(embeddingCollection.exposeMap()).isEmpty();

        var embedding = makeEmbedding("A", API_KEY);
        embedding.setId(UUID.randomUUID());
        embeddingCollection.addEmbedding(embedding);

        assertThat(embeddingCollection.exposeMap()).hasSize(1);
        assertThat(embeddingCollection.exposeMap().get("A"))
                .containsEntry(embedding.getId(), MatrixUtils.createRealVector(embedding.getEmbedding()));
    }

    @Test
    void testCreate() {
        var projection1 = makeEnhancedEmbeddingProjection("A");
        var projection2 = makeEnhancedEmbeddingProjection("B");
        var projection3 = makeEnhancedEmbeddingProjection("C");
        var projections = new EnhancedEmbeddingProjection[]{projection1, projection2, projection3};
        var embeddingCollection = EmbeddingCollection.from(Arrays.asList(projections));

        assertThat(embeddingCollection.exposeMap()).hasSize(projections.length);
        assertThat(embeddingCollection.exposeMap().get("A"))
                .containsEntry(projection1.getEmbeddingId(), MatrixUtils.createRealVector(projection1.getEmbeddingData()));
        assertThat(embeddingCollection.exposeMap().get("B"))
                .containsEntry(projection2.getEmbeddingId(), MatrixUtils.createRealVector(projection2.getEmbeddingData()));
        assertThat(embeddingCollection.exposeMap().get("C"))
                .containsEntry(projection3.getEmbeddingId(), MatrixUtils.createRealVector(projection3.getEmbeddingData()));
    }

    @Test
    void testAdd() {
        var projections = new EnhancedEmbeddingProjection[]{
                makeEnhancedEmbeddingProjection("A"),
                makeEnhancedEmbeddingProjection("B"),
                makeEnhancedEmbeddingProjection("C")
        };
        var embeddingCollection = EmbeddingCollection.from(Arrays.asList(projections));
        var newEmbedding = makeEmbedding("D", API_KEY);
        newEmbedding.setId(UUID.randomUUID());

        var projection = embeddingCollection.addEmbedding(newEmbedding);
        assertThat(projection).isNotNull();

        assertThat(embeddingCollection.exposeMap()).hasSize(projections.length + 1);
        assertThat(embeddingCollection.exposeMap().get("D"))
                .containsEntry(newEmbedding.getId(), MatrixUtils.createRealVector(newEmbedding.getEmbedding()));
    }

    @Test
    void testRemove() {
        var projection1 = makeEnhancedEmbeddingProjection("A");
        var projection2 = makeEnhancedEmbeddingProjection("B");
        var projection3 = makeEnhancedEmbeddingProjection("C");
        var projections = new EnhancedEmbeddingProjection[]{projection1, projection2, projection3};
        var embeddingCollection = EmbeddingCollection.from(Arrays.asList(projections));

        embeddingCollection.removeEmbedding(EmbeddingProjection.from(projection1));

        assertThat(embeddingCollection.exposeMap()).hasSize(projections.length - 1);
        assertThat(embeddingCollection.exposeMap().get("B"))
                .containsEntry(projection2.getEmbeddingId(), MatrixUtils.createRealVector(projection2.getEmbeddingData()));
        assertThat(embeddingCollection.exposeMap().get("C"))
                .containsEntry(projection3.getEmbeddingId(), MatrixUtils.createRealVector(projection3.getEmbeddingData()));
    }
}