package com.exadel.frs.core.trainservice.component.classifiers;

import com.exadel.frs.commonservice.entity.EnhancedEmbeddingProjection;
import com.exadel.frs.commonservice.sdk.faces.FacesApiClient;
import com.exadel.frs.commonservice.sdk.faces.feign.dto.FacesStatusResponse;
import com.exadel.frs.core.trainservice.cache.EmbeddingCacheProvider;
import com.exadel.frs.core.trainservice.cache.EmbeddingCollection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.math3.linear.MatrixUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EuclideanDistanceClassifierTest {
    private static final EnhancedEmbeddingProjection EMBEDDING_1 =
            new EnhancedEmbeddingProjection(UUID.randomUUID(), normalize(new double[]{-0.17530868400507033, -0.09430193935596984, -0.055193605419157174}), "A");
    private static final EnhancedEmbeddingProjection EMBEDDING_2 =
            new EnhancedEmbeddingProjection(UUID.randomUUID(), normalize(new double[]{-0.05813901327886949, -0.10933345187358641, 0.009466606891908468}), "B");
    private static final EnhancedEmbeddingProjection EMBEDDING_3 =
            new EnhancedEmbeddingProjection(UUID.randomUUID(), normalize(new double[]{0.15234856133696190, 0.07509153805882289, -0.010386721076615821}), "C");
    private static final double[] ALMOST_EMBEDDING_1_INPUT = new double[]{-0.17530868400507032, -0.09430193935596983, -0.055193605419157172};
    @Mock
    EmbeddingCacheProvider provider;
    @Mock
    FacesApiClient facesApiClient;
    @InjectMocks
    EuclideanDistanceClassifier classifier;

    @Test
    void predict() {
        // arrange
        Mockito.when(provider.getOrLoad(Mockito.anyString())).thenReturn(
                EmbeddingCollection.from(
                        List.of(
                                EMBEDDING_1,
                                EMBEDDING_2,
                                EMBEDDING_3
                        )
                )
        );
        Mockito.when(facesApiClient.getStatus()).thenReturn(
                new FacesStatusResponse().setSimilarityCoefficients(List.of(3.9, 1.0))
        );

        // act
        var result = classifier.predict(ALMOST_EMBEDDING_1_INPUT, "api_key", 1);

        // assert
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).getRight()).isEqualTo(EMBEDDING_1.getSubjectName());
        Assertions.assertThat(result.get(0).getLeft()).isCloseTo(0.999, Assertions.offset(0.001));
    }

    @Test
    void verify() {
        // arrange
        Mockito.when(provider.getOrLoad(Mockito.anyString())).thenReturn(
                EmbeddingCollection.from(
                        List.of(
                                EMBEDDING_1,
                                EMBEDDING_2,
                                EMBEDDING_3
                        )
                )
        );
        Mockito.when(facesApiClient.getStatus()).thenReturn(
                new FacesStatusResponse().setSimilarityCoefficients(List.of(3.9, 1.0))
        );

        // act
        var result = classifier.verify(ALMOST_EMBEDDING_1_INPUT, "api_key", EMBEDDING_1.getEmbeddingId());

        // assert
        Assertions.assertThat(result).isCloseTo(0.999, Assertions.offset(0.001));
    }

    @Test
    void verifySubject() {
        // arrange
        Mockito.when(provider.getEmbeddings(Mockito.anyString(), Mockito.anyString())).thenReturn(
                Optional.of(
                        Map.of(
                                EMBEDDING_1.getEmbeddingId(), MatrixUtils.createRealVector(EMBEDDING_1.getEmbeddingData()),
                                EMBEDDING_2.getEmbeddingId(), MatrixUtils.createRealVector(EMBEDDING_2.getEmbeddingData()),
                                EMBEDDING_3.getEmbeddingId(), MatrixUtils.createRealVector(EMBEDDING_3.getEmbeddingData())
                        )
                )
        );
        Mockito.when(facesApiClient.getStatus()).thenReturn(
                new FacesStatusResponse().setSimilarityCoefficients(List.of(3.9, 1.0))
        );

        // act
        var result = classifier.verifySubject("api_key", ALMOST_EMBEDDING_1_INPUT, EMBEDDING_1.getSubjectName(), 3);

        // assert
        Assertions.assertThat(result).hasSize(3);
        Assertions.assertThat(result.get(0).getLeft()).isEqualTo(EMBEDDING_1.getEmbeddingId());
        Assertions.assertThat(result.get(1).getLeft()).isEqualTo(EMBEDDING_2.getEmbeddingId());
        Assertions.assertThat(result.get(2).getLeft()).isEqualTo(EMBEDDING_3.getEmbeddingId());
    }

    @ParameterizedTest
    @CsvSource({
            "1, 1, 1",
            "1, 2, 3",
            "2, 1, 4",
            "0, 1, 0",
            "1, 0, 0",
            "0, 0, 1"
    })
    void normalizeOne(int first, int second, int third) {
        // arrange
        double[] input = new double[]{first, second, third};

        // act
        double[] normalized = classifier.normalizeOne(input);

        // assert
        Assertions.assertThat(normalized)
                .isEqualTo(normalize(input));
        Mockito.verify(facesApiClient, Mockito.never()).getStatus();
    }

    static double[] normalize(double[] input) {
        return MatrixUtils.createRealVector(input).unitVector().toArray();
    }
}
