package com.exadel.frs.core.trainservice.service;

import com.exadel.frs.commonservice.sdk.faces.FacesApiClient;
import com.exadel.frs.commonservice.sdk.faces.feign.dto.FindFacesResponse;
import com.exadel.frs.commonservice.sdk.faces.feign.dto.FindFacesResult;
import com.exadel.frs.commonservice.sdk.faces.feign.dto.PluginsVersions;
import com.exadel.frs.core.trainservice.component.FaceClassifierPredictor;
import com.exadel.frs.core.trainservice.dto.EmbeddingSimilaritiesDto.EmbeddingSimilarityDto;
import com.exadel.frs.core.trainservice.dto.ProcessImageParams;
import com.exadel.frs.core.trainservice.dto.SubjectVerificationResponseDto;
import com.exadel.frs.core.trainservice.mapper.FacesMapper;
import com.exadel.frs.core.trainservice.mapper.FacesMapperImpl;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.exadel.frs.core.trainservice.system.global.Constants.PREDICTION_COUNT;
import static com.exadel.frs.core.trainservice.system.global.Constants.SUBJECT;


class SubjectVerificationProcessServiceImplTest {
    private static final Double[] EMBEDDING = {1.0, 2.0, 3.0};
    private static final String SUBJECT_NAME = "subject";
    private static final UUID EMBEDDING_ID_1 = UUID.randomUUID();
    private static final UUID EMBEDDING_ID_2 = UUID.randomUUID();
    FacesApiClient facesApiClient = Mockito.mock(FacesApiClient.class);
    FaceClassifierPredictor classifierPredictor = Mockito.mock(FaceClassifierPredictor.class);
    FacesMapper facesMapper = new FacesMapperImpl();
    SubjectVerificationProcessServiceImpl service = new SubjectVerificationProcessServiceImpl(
            facesApiClient,
            facesMapper,
            classifierPredictor
    );


    @Test
    void processImage() {
        // arrange
        var processImageParams = ProcessImageParams.builder()
                .additionalParams(Map.of(SUBJECT, SUBJECT_NAME, PREDICTION_COUNT, 1))
                .imageBase64("image")
                .apiKey("api_key")
                .detProbThreshold(0.5)
                .facePlugins("face_plugins")
                .status(true)
                .limit(1)
                .build();
        var facesResult = new FindFacesResult();
        facesResult.setEmbedding(EMBEDDING);
        Mockito.when(facesApiClient.findFacesBase64WithCalculator(
                processImageParams.getImageBase64(),
                processImageParams.getLimit(),
                processImageParams.getDetProbThreshold(),
                processImageParams.getFacePlugins()
        )).thenReturn(new FindFacesResponse(new PluginsVersions(), List.of(facesResult)));
        Mockito.when(classifierPredictor.verifySubject(
                "api_key",
                Arrays.stream(EMBEDDING).mapToDouble(Double::doubleValue).toArray(),
                SUBJECT_NAME,
                1
        )).thenReturn(List.of(Pair.of(EMBEDDING_ID_1, 0.99), Pair.of(EMBEDDING_ID_2, 0.98)));

        // act
        var result = (SubjectVerificationResponseDto) service.processImage(processImageParams);


        // assert
        Mockito.verify(facesApiClient, Mockito.times(1)).findFacesBase64WithCalculator(
                processImageParams.getImageBase64(),
                processImageParams.getLimit(),
                processImageParams.getDetProbThreshold(),
                processImageParams.getFacePlugins()
        );
        Mockito.verify(classifierPredictor, Mockito.times(1)).verifySubject(
                "api_key",
                Arrays.stream(EMBEDDING).mapToDouble(Double::doubleValue).toArray(),
                SUBJECT_NAME,
                1
        );
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getPluginsVersions()).isEqualTo(facesMapper.toPluginVersionsDto(new PluginsVersions()));
        Assertions.assertThat(result.getResult().getEmbeddings())
                .containsOnly(
                        new EmbeddingSimilarityDto(EMBEDDING_ID_1, BigDecimal.valueOf(0.99)),
                        new EmbeddingSimilarityDto(EMBEDDING_ID_2, BigDecimal.valueOf(0.98))
                );
    }
}
