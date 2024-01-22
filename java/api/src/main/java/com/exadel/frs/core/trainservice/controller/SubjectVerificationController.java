package com.exadel.frs.core.trainservice.controller;

import com.exadel.frs.core.trainservice.dto.Base64File;
import com.exadel.frs.core.trainservice.dto.FaceProcessResponse;
import com.exadel.frs.core.trainservice.dto.ProcessImageParams;
import com.exadel.frs.core.trainservice.service.SubjectVerificationProcessServiceImpl;
import com.exadel.frs.core.trainservice.validation.ImageExtensionValidator;
import io.swagger.annotations.ApiParam;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.exadel.frs.commonservice.system.global.Constants.DET_PROB_THRESHOLD;
import static com.exadel.frs.core.trainservice.system.global.Constants.API_KEY_DESC;
import static com.exadel.frs.core.trainservice.system.global.Constants.API_V1;
import static com.exadel.frs.core.trainservice.system.global.Constants.DET_PROB_THRESHOLD_DESC;
import static com.exadel.frs.core.trainservice.system.global.Constants.FACE_PLUGINS;
import static com.exadel.frs.core.trainservice.system.global.Constants.FACE_PLUGINS_DESC;
import static com.exadel.frs.core.trainservice.system.global.Constants.PREDICTION_COUNT;
import static com.exadel.frs.core.trainservice.system.global.Constants.PREDICTION_COUNT_DEFAULT_VALUE;
import static com.exadel.frs.core.trainservice.system.global.Constants.PREDICTION_COUNT_DESC;
import static com.exadel.frs.core.trainservice.system.global.Constants.PREDICTION_COUNT_MIN_DESC;
import static com.exadel.frs.core.trainservice.system.global.Constants.PREDICTION_COUNT_REQUEST_PARAM;
import static com.exadel.frs.core.trainservice.system.global.Constants.STATUS;
import static com.exadel.frs.core.trainservice.system.global.Constants.STATUS_DEFAULT_VALUE;
import static com.exadel.frs.core.trainservice.system.global.Constants.STATUS_DESC;
import static com.exadel.frs.core.trainservice.system.global.Constants.SUBJECT;
import static com.exadel.frs.core.trainservice.system.global.Constants.SUBJECT_DESC;
import static com.exadel.frs.core.trainservice.system.global.Constants.X_FRS_API_KEY_HEADER;

@Validated
@RestController
@RequestMapping(API_V1 + "/recognition/subjects")
@RequiredArgsConstructor
public class SubjectVerificationController {
    private static final int PREDICTION_COUNT_MAX = 1000;
    private static final int PREDICTION_COUNT_MIN_VALUE = 1;
    private static final int LIMIT = 1;
    private static final String DEFAULT_FACE_PLUGINS = "";

    private final ImageExtensionValidator imageValidator;
    private final SubjectVerificationProcessServiceImpl verificationService;

    @PostMapping(value = "/{subjectName}/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FaceProcessResponse verifySubjectBase64(
            @ApiParam(value = API_KEY_DESC, required = true)
            @RequestHeader(X_FRS_API_KEY_HEADER)
            final String apiKey,
            @ApiParam(value = SUBJECT_DESC, required = true)
            @PathVariable
            final String subjectName,
            @ApiParam(value = PREDICTION_COUNT_DESC)
            @RequestParam(name = PREDICTION_COUNT_REQUEST_PARAM, defaultValue = PREDICTION_COUNT_DEFAULT_VALUE, required = false)
            @Min(value = PREDICTION_COUNT_MIN_VALUE, message = PREDICTION_COUNT_MIN_DESC)
            @Max(value = PREDICTION_COUNT_MAX)
            final Integer predictionCount,
            @ApiParam(value = DET_PROB_THRESHOLD_DESC)
            @RequestParam(value = DET_PROB_THRESHOLD, required = false)
            final Double detProbThreshold,
            @ApiParam(value = FACE_PLUGINS_DESC)
            @RequestParam(value = FACE_PLUGINS, required = false, defaultValue = DEFAULT_FACE_PLUGINS)
            final String facePlugins,
            @ApiParam(value = STATUS_DESC)
            @RequestParam(value = STATUS, required = false, defaultValue = STATUS_DEFAULT_VALUE)
            final Boolean status,
            @RequestBody @Valid Base64File request
    ) {
        imageValidator.validateBase64(request.getContent());

        var processImageParams = ProcessImageParams.builder()
                .additionalParams(Map.of(SUBJECT, subjectName, PREDICTION_COUNT, predictionCount))
                .apiKey(apiKey)
                .detProbThreshold(detProbThreshold)
                .imageBase64(request.getContent())
                .facePlugins(facePlugins)
                .limit(LIMIT)
                .status(status)
                .build();

        return verificationService.processImage(processImageParams);
    }
}
