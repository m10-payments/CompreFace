package com.exadel.frs.core.trainservice.util;

import org.springframework.lang.NonNull;

public final class MaskUtils {
    private MaskUtils() {
        // NOOP
    }

    public static String maskApiKey(@NonNull String apiKey) {
        var maskedApiKey = new StringBuilder();
        for (int i = 0; i < apiKey.length(); i++) {
            if (i < 4 || apiKey.charAt(i) == '-') {
                maskedApiKey.append(apiKey.charAt(i));
            } else {
                maskedApiKey.append('*');
            }
        }
        return maskedApiKey.toString();
    }
}
