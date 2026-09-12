package com.runningolle.domain.home.repository;

import com.runningolle.domain.home.entity.CourseRecommendationDocument;
import com.runningolle.domain.home.entity.RecommendationDocumentSourceType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRecommendationDocumentRepository extends JpaRepository<CourseRecommendationDocument, UUID> {

    @EntityGraph(attributePaths = "course")
    List<CourseRecommendationDocument> findByCourse_IdInAndIsDeletedFalseOrderByCourse_IdAscCreatedAtDesc(
            Collection<UUID> courseIds
    );

    @EntityGraph(attributePaths = "course")
    List<CourseRecommendationDocument> findByCourse_IdOrderByCreatedAtDesc(UUID courseId);

    @EntityGraph(attributePaths = "course")
    List<CourseRecommendationDocument> findByCourse_IdAndSourceTypeOrderByCreatedAtDesc(
            UUID courseId,
            RecommendationDocumentSourceType sourceType
    );

    Optional<CourseRecommendationDocument> findByCourse_IdAndSourceTypeAndSourceKeyAndIsDeletedFalse(
            UUID courseId,
            RecommendationDocumentSourceType sourceType,
            String sourceKey
    );

    Optional<CourseRecommendationDocument> findByCourse_IdAndSourceTypeAndSourceKey(
            UUID courseId,
            RecommendationDocumentSourceType sourceType,
            String sourceKey
    );
}
