package com.exadel.frs.core.trainservice.dto;

import com.exadel.frs.commonservice.dto.PluginsVersionsDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(NON_NULL)
public class SubjectVerificationResponseDto extends FaceProcessResponse {

    private EmbeddingSimilaritiesDto result;
    @JsonProperty(value = "plugins_versions")
    private PluginsVersionsDto pluginsVersions;

    @Override
    public SubjectVerificationResponseDto prepareResponse(ProcessImageParams processImageParams) {
        if (!Boolean.TRUE.equals(processImageParams.getStatus())) {
            setPluginsVersions(null);
            result.setExecutionTime(null);
        }
        var facePlugins = processImageParams.getFacePlugins();
        if (StringUtils.isBlank(facePlugins) || !facePlugins.contains(CALCULATOR)) {
            result.setEmbedding(null);
        }
        return this;
    }
}
