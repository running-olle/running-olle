package com.runningolle.domain.home.service;

import com.runningolle.domain.home.service.CourseRecommendationReranker.BaseRecommendationCandidate;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RecommendationUserPreference;
import java.util.List;
import java.util.UUID;

/**
 * Retrieves bounded unstructured context only for the already-selected recommendation candidates.
 * Phase 4 can replace the backing implementation with pgvector retrieval without changing rerank flow.
 */
public interface CourseRecommendationContextRetriever {

    List<RetrievedRecommendationContext> retrieve(
            RecommendationUserPreference preference,
            List<BaseRecommendationCandidate> candidates
    );

    record RetrievedRecommendationContext(
            String documentId,
            UUID courseId,
            String sourceType,
            String content
    ) {
    }
}
