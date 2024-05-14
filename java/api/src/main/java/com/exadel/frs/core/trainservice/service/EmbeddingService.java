package com.exadel.frs.core.trainservice.service;

import com.exadel.frs.commonservice.entity.Embedding;
import com.exadel.frs.commonservice.entity.EmbeddingProjection;
import com.exadel.frs.commonservice.entity.EnhancedEmbeddingProjection;
import com.exadel.frs.commonservice.entity.Img;
import com.exadel.frs.commonservice.repository.EmbeddingRepository;
import com.exadel.frs.commonservice.repository.ImgRepository;
import com.exadel.frs.core.trainservice.system.global.Constants;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingRepository embeddingRepository;
    private final ImgRepository imgRepository;

    @Transactional
    public int updateEmbedding(UUID embeddingId, double[] embedding, String calculator) {
        return embeddingRepository.updateEmbedding(embeddingId, embedding, calculator);
    }

    @Transactional
    public void updateEmbedding(UUID embeddingId, Map<String, String> imageAttributes) {
        embeddingRepository.findById(embeddingId)
                .ifPresent(em -> {
                    var img = em.getImg();
                    var attrs = img.getAttributes();
                    if (attrs == null) {
                        attrs = imageAttributes;
                    } else {
                        attrs.putAll(imageAttributes);
                    }
                    img.setAttributes(attrs);
                });
    }


    public void doWithEnhancedEmbeddingProjections(
            String apiKey,
            Consumer<EnhancedEmbeddingProjection> func,
            int pageSize
    ) {
        var start = Instant.now();
        var page = embeddingRepository.findEnhancedBySubjectApiKey(apiKey, Pageable.ofSize(pageSize));
        page.forEach(func);

        log.debug("Loaded {} / {} embeddings for API key {}", page.getSize(), page.getTotalElements(), apiKey);

        while (page.hasNext()) {
            page = embeddingRepository.findEnhancedBySubjectApiKey(apiKey, page.nextPageable());
            log.debug("Loaded {} / {} embeddings for API key {}", page.getSize(), page.getTotalElements(), apiKey);
            page.forEach(func);
        }
        var totalDuration = Duration.between(start, Instant.now());
        log.debug("Finished loading {} embeddings for API key {} in {}", page.getTotalElements(), apiKey, totalDuration);
    }

    public List<Embedding> getWithImgAndCalculatorNotEq(String calculator) {
        return embeddingRepository.getWithImgAndCalculatorNotEq(calculator);
    }

    public Optional<Img> getImg(Embedding embedding) {
        return Optional.ofNullable(embedding.getImg().getId())
                .flatMap(imgRepository::findById);
    }

    public Optional<Img> getImg(String apiKey, UUID embeddingId) {
        return imgRepository.getImgByEmbeddingId(apiKey, embeddingId);
    }

    public Page<EmbeddingProjection> listEmbeddings(String apiKey, String subjectName, Pageable pageable) {
        return embeddingRepository.findBySubjectApiKeyAndSubjectName(apiKey, subjectName, pageable);
    }

    public boolean isDemoCollectionInconsistent() {
        return embeddingRepository.countBySubjectApiKeyAndCalculatorNotEq(
                Constants.DEMO_API_KEY,
                Constants.FACENET2018
        ) > 0;
    }

    public boolean isDbInconsistent(String currentCalculator) {
        return embeddingRepository.countBySubjectApiKeyNotEqAndCalculatorNotEq(
                Constants.DEMO_API_KEY,
                currentCalculator
        ) > 0;
    }
}
