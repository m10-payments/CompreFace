package com.exadel.frs.core.trainservice.dto;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AddEmbeddingRequest extends Base64File {
    @Nullable
    private Map<String, String> imageAttributes;
}
