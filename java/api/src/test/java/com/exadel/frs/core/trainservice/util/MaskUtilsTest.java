package com.exadel.frs.core.trainservice.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskUtilsTest {

    @Test
    void maskApiKey() {
        // arrange
        var apiKey = "2abca51f-af81-43a8-81de-0dfc75e94b23";

        // act
        var masked = MaskUtils.maskApiKey(apiKey);

        // assert
        assertEquals("2abc****-****-****-****-************", masked);
    }
}
