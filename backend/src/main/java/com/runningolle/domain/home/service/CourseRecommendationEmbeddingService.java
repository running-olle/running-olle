package com.runningolle.domain.home.service;

import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.entity.CourseRecommendationDocument;
import com.runningolle.domain.home.repository.CourseRecommendationDocumentRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseRecommendationEmbeddingService {

    private final CourseRecommendationDocumentRepository courseRecommendationDocumentRepository;
    private final HomeRecommendationProperties homeRecommendationProperties;
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    @Value("${spring.ai.model.embedding:none}")
    private String embeddingModel;

    @Transactional
    public SyncResult syncCourseDescriptionEmbedding(UUID courseId) {
        if (!homeRecommendationProperties.getRag().isEnabled()) {
            return SyncResult.SKIPPED_RAG_DISABLED;
        }

        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return SyncResult.SKIPPED_VECTOR_STORE_UNAVAILABLE;
        }

        Optional<CourseRecommendationDocument> documentOptional =
                courseRecommendationDocumentRepository.findByCourse_IdAndSourceTypeAndSourceKey(
                        courseId,
                        CourseRecommendationDocumentService.COURSE_DESCRIPTION_SOURCE_TYPE,
                        CourseRecommendationDocumentService.COURSE_DESCRIPTION_SOURCE_KEY
                );
        if (documentOptional.isEmpty()) {
            return SyncResult.SKIPPED_DOCUMENT_NOT_FOUND;
        }

        return syncDocumentEmbedding(vectorStore, courseId, documentOptional.get());
    }

    @Transactional
    public List<SyncResult> syncCourseRecommendationEmbeddings(UUID courseId) {
        if (!homeRecommendationProperties.getRag().isEnabled()) {
            return List.of(SyncResult.SKIPPED_RAG_DISABLED);
        }

        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return List.of(SyncResult.SKIPPED_VECTOR_STORE_UNAVAILABLE);
        }

        List<CourseRecommendationDocument> documents =
                courseRecommendationDocumentRepository.findByCourse_IdOrderByCreatedAtDesc(courseId);
        if (documents.isEmpty()) {
            return List.of(SyncResult.SKIPPED_DOCUMENT_NOT_FOUND);
        }

        List<SyncResult> results = new ArrayList<>(documents.size());
        for (CourseRecommendationDocument document : documents) {
            results.add(syncDocumentEmbedding(vectorStore, courseId, document));
        }
        return results;
    }

    private SyncResult syncDocumentEmbedding(
            VectorStore vectorStore,
            UUID courseId,
            CourseRecommendationDocument document
    ) {
        try {
            vectorStore.delete(List.of(document.getId().toString()));

            if (Boolean.TRUE.equals(document.getIsDeleted()) || !StringUtils.hasText(document.getContent())) {
                return SyncResult.DELETED_FROM_VECTOR_STORE;
            }

            vectorStore.add(List.of(toDocument(document)));
            document.markEmbeddingCompleted(resolveEmbeddingModel(), LocalDateTime.now());
            return SyncResult.SYNCED;
        } catch (RuntimeException exception) {
            document.markEmbeddingFailed(exception.getMessage());
            log.warn(
                    "Failed to sync recommendation embedding. courseId={}, documentId={}",
                    courseId,
                    document.getId(),
                    exception
            );
            return SyncResult.FAILED;
        }
    }

    private Document toDocument(CourseRecommendationDocument document) {
        Document.Builder builder = Document.builder()
                .id(document.getId().toString())
                .text(document.getContent())
                .metadata("documentId", document.getId().toString())
                .metadata("courseId", document.getCourse().getId().toString())
                .metadata("sourceType", document.getSourceType().name());

        if (document.getMetadata() != null && document.getMetadata().isObject()) {
            document.getMetadata().fields()
                    .forEachRemaining(entry -> builder.metadata(entry.getKey(), metadataValue(entry.getValue())));
        }
        return builder.build();
    }

    private Object metadataValue(com.fasterxml.jackson.databind.JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isArray()) {
            List<String> values = new ArrayList<>();
            value.forEach(item -> values.add(item.asText()));
            return values;
        }
        if (value.isNumber()) {
            return value.numberValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        return value.asText();
    }

    private String resolveEmbeddingModel() {
        return StringUtils.hasText(embeddingModel) ? embeddingModel : "unknown";
    }

    public enum SyncResult {
        SYNCED,
        DELETED_FROM_VECTOR_STORE,
        SKIPPED_RAG_DISABLED,
        SKIPPED_VECTOR_STORE_UNAVAILABLE,
        SKIPPED_DOCUMENT_NOT_FOUND,
        FAILED
    }
}
