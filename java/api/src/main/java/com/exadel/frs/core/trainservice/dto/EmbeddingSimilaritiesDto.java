package com.exadel.frs.core.trainservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(NON_NULL)
public class EmbeddingSimilaritiesDto extends FindFacesResultDto {
    List<EmbeddingSimilarityDto> embeddings;

    @Value
    @JsonInclude(NON_NULL)
    public static class EmbeddingSimilarityDto {
        UUID embeddingId;
        BigDecimal similarity;
    }
}
