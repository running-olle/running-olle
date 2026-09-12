package com.runningolle.domain.home.service;

import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.entity.CourseRecommendationDocument;
import com.runningolle.domain.home.entity.RecommendationDocumentEmbeddingStatus;
import com.runningolle.domain.home.repository.CourseRecommendationDocumentRepository;
import com.runningolle.domain.home.service.CourseRecommendationContextRetriever.RetrievedRecommendationContext;
import com.runningolle.domain.home.service.CourseRecommendationReranker.BaseRecommendationCandidate;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RecommendationUserPreference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder.Op;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoredRecommendationDocumentContextRetriever implements CourseRecommendationContextRetriever {

    private static final String FALLBACK_SOURCE_TYPE = "COURSE_DESCRIPTION";

    private final CourseRecommendationDocumentRepository courseRecommendationDocumentRepository;
    private final HomeRecommendationProperties homeRecommendationProperties;
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    @Override
    public List<RetrievedRecommendationContext> retrieve(
            RecommendationUserPreference preference,
            List<BaseRecommendationCandidate> candidates
    ) {
        int limit = Math.min(candidates.size(), homeRecommendationProperties.getRag().getCandidateDocumentLimit());
        if (limit == 0) {
            return List.of();
        }

        Map<UUID, List<RetrievedRecommendationContext>> storedContextByCourseId =
                storedContextByCourseId(candidates.stream().map(BaseRecommendationCandidate::courseId).toList());
        if (homeRecommendationProperties.getRag().isEnabled()) {
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore != null) {
                try {
                    List<RetrievedRecommendationContext> vectorResults =
                            similaritySearch(vectorStore, preference, candidates, limit);
                    if (!vectorResults.isEmpty()) {
                        return vectorResults;
                    }
                } catch (RuntimeException exception) {
                    log.warn("Recommendation vector search failed. Falling back to stored recommendation contexts.", exception);
                }
            }
        }

        List<RetrievedRecommendationContext> contexts = new ArrayList<>(limit);
        for (BaseRecommendationCandidate candidate : candidates) {
            if (contexts.size() >= limit) {
                break;
            }

            List<RetrievedRecommendationContext> storedContexts =
                    storedContextByCourseId.getOrDefault(candidate.courseId(), List.of());
            if (!storedContexts.isEmpty()) {
                contexts.addAll(trimToRemainingCapacity(storedContexts, limit - contexts.size()));
                continue;
            }
            if (!StringUtils.hasText(candidate.description())) {
                continue;
            }
            contexts.add(new RetrievedRecommendationContext(
                    fallbackDocumentId(candidate.courseId()),
                    candidate.courseId(),
                    FALLBACK_SOURCE_TYPE,
                    candidate.description().trim()
            ));
        }
        return contexts;
    }

    private List<RetrievedRecommendationContext> similaritySearch(
            VectorStore vectorStore,
            RecommendationUserPreference preference,
            List<BaseRecommendationCandidate> candidates,
            int limit
    ) {
        SearchRequest.Builder requestBuilder = SearchRequest.builder()
                .query(buildSearchQuery(preference))
                .topK(limit);
        double similarityThreshold = homeRecommendationProperties.getRag().getSimilarityThreshold();
        if (similarityThreshold == 0.0) {
            requestBuilder.similarityThresholdAll();
        } else {
            requestBuilder.similarityThreshold(similarityThreshold);
        }
        if (homeRecommendationProperties.getRag().isMetadataFilterRequired()) {
            requestBuilder.filterExpression(courseIdFilterExpression(candidates));
        }

        return vectorStore.similaritySearch(requestBuilder.build()).stream()
                .map(this::toRetrievedRecommendationContext)
                .filter(context -> context.courseId() != null && StringUtils.hasText(context.content()))
                .filter(context -> candidates.stream().anyMatch(candidate -> candidate.courseId().equals(context.courseId())))
                .toList();
    }

    private Expression courseIdFilterExpression(List<BaseRecommendationCandidate> candidates) {
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        List<String> courseIds = candidates.stream()
                .map(candidate -> candidate.courseId().toString())
                .toList();
        Op expression = filterBuilder.eq("courseId", courseIds.get(0));
        for (int i = 1; i < courseIds.size(); i++) {
            expression = filterBuilder.or(expression, filterBuilder.eq("courseId", courseIds.get(i)));
        }
        return expression.build();
    }

    private Map<UUID, List<RetrievedRecommendationContext>> storedContextByCourseId(Collection<UUID> courseIds) {
        Map<UUID, List<RetrievedRecommendationContext>> contextsByCourseId = new LinkedHashMap<>();
        for (CourseRecommendationDocument document :
                courseRecommendationDocumentRepository.findByCourse_IdInAndIsDeletedFalseOrderByCourse_IdAscCreatedAtDesc(courseIds)) {
            if (!StringUtils.hasText(document.getContent())) {
                continue;
            }
            if (homeRecommendationProperties.getRag().isEnabled()
                    && document.getEmbeddingStatus() != RecommendationDocumentEmbeddingStatus.COMPLETED) {
                continue;
            }
            contextsByCourseId.computeIfAbsent(document.getCourse().getId(), ignored -> new ArrayList<>())
                    .add(new RetrievedRecommendationContext(
                            document.getId().toString(),
                            document.getCourse().getId(),
                            document.getSourceType().name(),
                            document.getContent().trim()
                    ));
        }
        return contextsByCourseId;
    }

    private List<RetrievedRecommendationContext> trimToRemainingCapacity(
            List<RetrievedRecommendationContext> contexts,
            int remainingCapacity
    ) {
        if (contexts.size() <= remainingCapacity) {
            return contexts;
        }
        return contexts.subList(0, remainingCapacity);
    }

    private RetrievedRecommendationContext toRetrievedRecommendationContext(org.springframework.ai.document.Document document) {
        Object courseId = document.getMetadata().get("courseId");
        if (courseId == null) {
            return new RetrievedRecommendationContext(document.getId(), null, String.valueOf(document.getMetadata().get("sourceType")), document.getText());
        }
        return new RetrievedRecommendationContext(
                document.getId(),
                UUID.fromString(String.valueOf(courseId)),
                String.valueOf(document.getMetadata().get("sourceType")),
                document.getText()
        );
    }

    private String buildSearchQuery(RecommendationUserPreference preference) {
        List<String> parts = new ArrayList<>();
        if (!preference.userTypes().isEmpty()) {
            parts.add("user types: " + preference.userTypes());
        }
        if (preference.preferredDistance() != null) {
            parts.add("preferred distance: " + preference.preferredDistance());
        }
        if (preference.preferredDifficulty() != null) {
            parts.add("preferred difficulty: " + preference.preferredDifficulty());
        }
        if (!preference.themeCodes().isEmpty()) {
            parts.add("preferred themes: " + preference.themeCodes());
        }
        if (parts.isEmpty()) {
            return "Jeju running course recommendation";
        }
        return String.join(", ", parts);
    }

    private String fallbackDocumentId(UUID courseId) {
        return "course-description-" + courseId;
    }
}
