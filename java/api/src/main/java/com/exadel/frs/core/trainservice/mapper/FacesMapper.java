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

package com.exadel.frs.core.trainservice.mapper;

import com.exadel.frs.commonservice.dto.PluginsVersionsDto;
import com.exadel.frs.commonservice.sdk.faces.exception.NoFacesFoundException;
import com.exadel.frs.commonservice.sdk.faces.feign.dto.FindFacesResponse;
import com.exadel.frs.commonservice.sdk.faces.feign.dto.FindFacesResult;
import com.exadel.frs.commonservice.sdk.faces.feign.dto.PluginsVersions;
import com.exadel.frs.core.trainservice.dto.EmbeddingSimilaritiesDto;
import com.exadel.frs.core.trainservice.dto.EmbeddingSimilaritiesDto.EmbeddingSimilarityDto;
import com.exadel.frs.core.trainservice.dto.FacesDetectionResponseDto;
import com.exadel.frs.core.trainservice.dto.FacesRecognitionResponseDto;
import com.exadel.frs.core.trainservice.dto.SubjectVerificationResponseDto;
import com.exadel.frs.core.trainservice.dto.VerifyFacesResultDto;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FacesMapper {

    FacesDetectionResponseDto toFacesDetectionResponseDto(FindFacesResponse facesResponse);

    FacesRecognitionResponseDto toFacesRecognitionResponseDto(FindFacesResponse facesResponse);

    SubjectVerificationResponseDto toSubjectVerificationResponseDto(FindFacesResponse facesResponse);

    default EmbeddingSimilaritiesDto map(List<FindFacesResult> value) {
        return value.stream().findFirst()
                .map(this::map)
                .orElseThrow(NoFacesFoundException::new);
    }

    @Mapping(target = "embeddings", ignore = true)
    EmbeddingSimilaritiesDto map(FindFacesResult value);

    List<EmbeddingSimilarityDto> toEmbeddingSimilarityDto(List<Pair<UUID, Double>> embeddings);

    @Mapping(target = "embeddingId", source = "left")
    @Mapping(target = "similarity", source = "right")
    EmbeddingSimilarityDto toEmbeddingSimilarityDto(Pair<UUID, Double> embedding);

    VerifyFacesResultDto toVerifyFacesResultDto(FindFacesResult facesResult);

    PluginsVersionsDto toPluginVersionsDto(PluginsVersions pluginsVersions);
}