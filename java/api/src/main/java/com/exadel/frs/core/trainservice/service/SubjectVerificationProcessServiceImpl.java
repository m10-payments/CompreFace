package com.exadel.frs.core.trainservice.service;

import com.exadel.frs.commonservice.sdk.faces.FacesApiClient;
import com.exadel.frs.commonservice.sdk.faces.exception.NoFacesFoundException;
import com.exadel.frs.commonservice.sdk.faces.feign.dto.FindFacesResponse;
import com.exadel.frs.core.trainservice.component.FaceClassifierPredictor;
import com.exadel.frs.core.trainservice.dto.FaceProcessResponse;
import com.exadel.frs.core.trainservice.dto.ProcessImageParams;
import com.exadel.frs.core.trainservice.dto.SubjectVerificationResponseDto;
import com.exadel.frs.core.trainservice.mapper.FacesMapper;
import java.util.Arrays;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.exadel.frs.core.trainservice.system.global.Constants.PREDICTION_COUNT;
import static com.exadel.frs.core.trainservice.system.global.Constants.SUBJECT;

@Service
@RequiredArgsConstructor
public class SubjectVerificationProcessServiceImpl implements FaceProcessService {
    private static final int FACE_LIMIT = 1;
    private final FacesApiClient facesApiClient;
    private final FacesMapper facesMapper;
    private final FaceClassifierPredictor classifierPredictor;

    @Override
    public FaceProcessResponse processImage(@NonNull ProcessImageParams processImageParams) {
        var subjectName = (String) processImageParams.getAdditionalParams().get(SUBJECT);
        return findFace(processImageParams)
                .map(response -> handleFindFaceResponse(processImageParams, subjectName, response))
                .orElseThrow(NoFacesFoundException::new);
    }

    /**
     * Find face on image (only the biggest one)
     *
     * @param processImageParams params for image processing
     * @return response from faces api client (find faces result and plugins versions)
     */
    private Optional<FindFacesResponse> findFace(ProcessImageParams processImageParams) {
        var response = facesApiClient.findFacesBase64WithCalculator(
                processImageParams.getImageBase64(),
                FACE_LIMIT,
                processImageParams.getDetProbThreshold(),
                processImageParams.getFacePlugins()
        );
        return response.getResult().isEmpty() ? Optional.empty() : Optional.of(response);
    }

    /**
     * Verify subject by face
     *
     * @param subjectName Subject name to verify against
     * @param response    Response from faces api client (find faces result and plugins versions)
     * @param params      Params for image processing
     * @return Subject verification response
     */
    private SubjectVerificationResponseDto handleFindFaceResponse(
            ProcessImageParams params,
            String subjectName,
            FindFacesResponse response
    ) {
        var apiKey = params.getApiKey();
        var input = Arrays.stream(getEmbedding(response)).mapToDouble(Double::doubleValue).toArray();
        var resultCount = (Integer) params.getAdditionalParams().get(PREDICTION_COUNT);
        var verificationResult = classifierPredictor.verifySubject(apiKey, input, subjectName, resultCount);

        var result = facesMapper.toSubjectVerificationResponseDto(response);
        result.getResult().setEmbeddings(facesMapper.toEmbeddingSimilarityDto(verificationResult));
        return result.prepareResponse(params);
    }

    private static Double[] getEmbedding(FindFacesResponse response) {
        return response.getResult().get(0).getEmbedding();
    }
}
