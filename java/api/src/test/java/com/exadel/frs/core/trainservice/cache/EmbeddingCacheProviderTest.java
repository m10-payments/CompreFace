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

import com.exadel.frs.commonservice.entity.Embedding;
import com.exadel.frs.commonservice.entity.EmbeddingProjection;
import com.exadel.frs.commonservice.entity.EnhancedEmbeddingProjection;
import com.exadel.frs.commonservice.entity.Subject;
import com.exadel.frs.core.trainservice.dto.CacheActionDto;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.AddEmbeddings;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.CacheAction;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.RemoveEmbeddings;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.RemoveSubjects;
import com.exadel.frs.core.trainservice.dto.CacheActionDto.RenameSubjects;
import com.exadel.frs.core.trainservice.service.EmbeddingService;
import com.exadel.frs.core.trainservice.service.NotificationReceiverService;
import com.exadel.frs.core.trainservice.service.NotificationSenderService;
import com.exadel.frs.core.trainservice.system.global.Constants;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.lang3.reflect.FieldUtils;

import static com.exadel.frs.core.trainservice.ItemsBuilder.makeEnhancedEmbeddingProjection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmbeddingCacheProviderTest {

    private static final String API_KEY = "model_key";
    private static final String SUBJECT_NAME = "subject_name";
    private static final String NEW_SUBJECT_NAME = "new_subject_name";
    private static final UUID EMBEDDING_ID_1 = UUID.randomUUID();
    private static final UUID EMBEDDING_ID_2 = UUID.randomUUID();
    private static final String TEST_CALCULATOR = "test-calculator";

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private NotificationSenderService notificationSenderService;

    @Mock
    private NotificationReceiverService notificationReceiverService;

    @InjectMocks
    private EmbeddingCacheProvider embeddingCacheProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void resetStaticCache() throws IllegalAccessException {
        FieldUtils.writeField(embeddingCacheProvider, "pageSize", 10, true);
        embeddingCacheProvider.invalidate(API_KEY);
        Mockito.doNothing().when(embeddingService).doWithEnhancedEmbeddingProjections(eq(API_KEY), any(), eq(10));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getOrLoad() {
        reset(embeddingService);
        var projections = new EnhancedEmbeddingProjection[]{
                makeEnhancedEmbeddingProjection("A"),
                makeEnhancedEmbeddingProjection("B"),
                makeEnhancedEmbeddingProjection("C")
        };

        Mockito.doAnswer(
            invocation -> {
                var function = (Consumer<EnhancedEmbeddingProjection>) invocation.getArgument(1);
                Arrays.stream(projections).forEach(function);
                return null;
            }
        ).when(embeddingService).doWithEnhancedEmbeddingProjections(eq(API_KEY), any(), eq(10));

        var actual = embeddingCacheProvider.getOrLoad(API_KEY);

        assertThat(actual, notNullValue());
        assertThat(actual.exposeMap(), notNullValue());
        assertThat(actual.exposeMap().size(), is(projections.length));
        assertThat(actual.exposeMap().get("A").size(), is(1));
        assertThat(actual.exposeMap().get("B").size(), is(1));
        assertThat(actual.exposeMap().get("C").size(), is(1));
    }

    @Test
    void removeEmbedding() {
        // arrange
        embeddingCacheProvider.addEmbedding(API_KEY, buildEmbedding(EMBEDDING_ID_1));
        embeddingCacheProvider.addEmbedding(API_KEY, buildEmbedding(EMBEDDING_ID_2));
        reset(notificationReceiverService);

        // act
        embeddingCacheProvider.removeEmbedding(API_KEY, new EmbeddingProjection(EMBEDDING_ID_1, SUBJECT_NAME));

        // assert
        var embeddings = embeddingCacheProvider.getOrLoad(API_KEY);
        Assertions.assertThat(embeddings.exposeMap()).hasSize(1);
        Assertions.assertThat(embeddings.exposeMap().get(SUBJECT_NAME)).hasSize(1);

        verify(notificationSenderService, times(1)).notifyCacheChange(
            buildCacheActionDto(CacheAction.REMOVE_EMBEDDINGS, new RemoveEmbeddings(Map.of(SUBJECT_NAME, List.of(EMBEDDING_ID_1))))
        );
    }

    @Test
    void updateSubjectName() {
        // arrange
        embeddingCacheProvider.addEmbedding(API_KEY, buildEmbedding(EMBEDDING_ID_1));
        embeddingCacheProvider.addEmbedding(API_KEY, buildEmbedding(EMBEDDING_ID_2));
        reset(notificationSenderService);

        // act
        embeddingCacheProvider.updateSubjectName(API_KEY, SUBJECT_NAME, NEW_SUBJECT_NAME);

        // assert
        var embeddings = embeddingCacheProvider.getOrLoad(API_KEY);
        Assertions.assertThat(embeddings.exposeMap()).hasSize(1);
        Assertions.assertThat(embeddings.exposeMap().get(NEW_SUBJECT_NAME)).hasSize(2);

        verify(notificationSenderService, times(1)).notifyCacheChange(
            buildCacheActionDto(CacheAction.RENAME_SUBJECTS, new RenameSubjects(Map.of(SUBJECT_NAME, NEW_SUBJECT_NAME)))
        );
    }

    @Test
    void removeBySubjectName() {
        // arrange
        embeddingCacheProvider.addEmbedding(API_KEY, buildEmbedding(EMBEDDING_ID_1));
        embeddingCacheProvider.addEmbedding(API_KEY, buildEmbedding(EMBEDDING_ID_2));
        reset(notificationSenderService);

        // act
        embeddingCacheProvider.removeBySubjectName(API_KEY, SUBJECT_NAME);

        // assert
        var embeddings = embeddingCacheProvider.getOrLoad(API_KEY);
        Assertions.assertThat(embeddings.exposeMap()).isEmpty();

        verify(notificationSenderService, times(1)).notifyCacheChange(
            buildCacheActionDto(CacheAction.REMOVE_SUBJECTS, new RemoveSubjects(List.of(SUBJECT_NAME)))
        );
    }

    @Test
    void addEmbedding() {
        // act
        embeddingCacheProvider.addEmbedding(API_KEY, buildEmbedding(EMBEDDING_ID_2));

        // assert
        var embeddings = embeddingCacheProvider.getOrLoad(API_KEY);
        Assertions.assertThat(embeddings.exposeMap()).hasSize(1);
        Assertions.assertThat(embeddings.exposeMap().get(SUBJECT_NAME)).hasSize(1);

        verify(notificationSenderService, times(1)).notifyCacheChange(
            buildCacheActionDto(CacheAction.ADD_EMBEDDINGS, new AddEmbeddings(List.of(EMBEDDING_ID_2)))
        );
    }

    @Test
    void invalidate() {
        // arrange
        embeddingCacheProvider.addEmbedding(API_KEY, buildEmbedding(EMBEDDING_ID_2));
        reset(notificationSenderService);

        // act
        embeddingCacheProvider.invalidate(API_KEY);

        // assert
        embeddingCacheProvider.exposeIfPresent(API_KEY, ec -> Assertions.assertThat(ec.exposeMap()).isEmpty());
        verify(notificationSenderService).notifyCacheChange(
            buildCacheActionDto(
                CacheAction.INVALIDATE,
                null
            )
        );
    }

    @Test
    void receivePutOnCache() {
        // arrange
        receiveInvalidateCache();
    }

    @Test
    void receiveInvalidateCache() {
        // arrange
        embeddingCacheProvider.addEmbedding(API_KEY, buildEmbedding(EMBEDDING_ID_2));

        // act
        embeddingCacheProvider.receiveInvalidateCache(API_KEY);

        // assert
        embeddingCacheProvider.exposeIfPresent(API_KEY, ec -> Assertions.assertThat(ec.exposeMap()).isEmpty());
    }

    private static <T> CacheActionDto<T> buildCacheActionDto(
        CacheAction cacheAction,
        T payload
    ) {
        return new CacheActionDto<>(
            cacheAction,
            API_KEY,
            Constants.SERVER_UUID,
            payload
        );
    }

    static Embedding buildEmbedding(
        UUID embeddingId
    ) {
        var subj = new Subject(
            UUID.randomUUID(),
            API_KEY,
            SUBJECT_NAME
        );
        return new Embedding(
            embeddingId,
            subj,
            new double[]{21.22, 222.444},
            TEST_CALCULATOR,
            null
        );
    }
}
