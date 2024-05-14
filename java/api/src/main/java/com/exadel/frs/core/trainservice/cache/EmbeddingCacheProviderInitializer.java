package com.exadel.frs.core.trainservice.cache;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingCacheProviderInitializer implements ApplicationRunner {
    private final EmbeddingCacheProvider embeddingCacheProvider;
    @Value("${embeddings.cache.initialization.api-keys}")
    private String initializationApiKeys;

    @Override
    public void run(ApplicationArguments args) {
        var apiKeys = Optional.ofNullable(initializationApiKeys)
                .map(keys -> keys.split(","))
                .stream()
                .flatMap(Arrays::stream)
                .filter(EmbeddingCacheProviderInitializer::isUuid)
                .collect(Collectors.toSet());
        embeddingCacheProvider.fillInCache(apiKeys);
    }

    private static boolean isUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID: {}", s, e);
            return false;
        }
    }
}
