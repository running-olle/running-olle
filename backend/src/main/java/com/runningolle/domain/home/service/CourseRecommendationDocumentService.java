package com.runningolle.domain.home.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseReview;
import com.runningolle.domain.course.entity.CourseTheme;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseReviewRepository;
import com.runningolle.domain.course.repository.CourseThemeRepository;
import com.runningolle.domain.home.entity.CourseRecommendationDocument;
import com.runningolle.domain.home.entity.RecommendationDocumentSourceType;
import com.runningolle.domain.home.repository.CourseRecommendationDocumentRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CourseRecommendationDocumentService {

    static final RecommendationDocumentSourceType COURSE_DESCRIPTION_SOURCE_TYPE =
            RecommendationDocumentSourceType.COURSE_DESCRIPTION;
    static final String COURSE_DESCRIPTION_SOURCE_KEY = "course-description";
    static final RecommendationDocumentSourceType COURSE_REVIEW_SOURCE_TYPE =
            RecommendationDocumentSourceType.COURSE_REVIEW;

    private final CourseRepository courseRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final CourseThemeRepository courseThemeRepository;
    private final CourseRecommendationDocumentRepository courseRecommendationDocumentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SyncResult syncCourseDescriptionDocument(UUID courseId) {
        Optional<Course> courseOptional = courseRepository.findByIdAndIsDeletedFalse(courseId);
        if (courseOptional.isEmpty()) {
            return SyncResult.SKIPPED_COURSE_NOT_FOUND;
        }

        Course course = courseOptional.get();
        Optional<CourseRecommendationDocument> existingDocument =
                courseRecommendationDocumentRepository.findByCourse_IdAndSourceTypeAndSourceKey(
                        courseId,
                        COURSE_DESCRIPTION_SOURCE_TYPE,
                        COURSE_DESCRIPTION_SOURCE_KEY
                );

        if (!StringUtils.hasText(course.getDescription())) {
            if (existingDocument.isPresent() && !Boolean.TRUE.equals(existingDocument.get().getIsDeleted())) {
                existingDocument.get().softDelete();
                return SyncResult.DELETED_EMPTY_CONTENT;
            }
            return SyncResult.SKIPPED_EMPTY_CONTENT;
        }

        String content = course.getDescription().trim();
        ObjectNode metadata = buildCourseMetadata(course);
        if (existingDocument.isPresent()) {
            if (existingDocument.get().hasSameActiveContent(course.getName(), content, metadata)) {
                return SyncResult.SKIPPED_UNCHANGED;
            }
            existingDocument.get().updateContent(course.getName(), content, metadata);
            return SyncResult.UPDATED;
        }

        courseRecommendationDocumentRepository.save(CourseRecommendationDocument.create(
                course,
                COURSE_DESCRIPTION_SOURCE_TYPE,
                COURSE_DESCRIPTION_SOURCE_KEY,
                course.getName(),
                content,
                metadata
        ));
        return SyncResult.CREATED;
    }

    @Transactional
    public ReviewSyncResult syncCourseReviewDocuments(UUID courseId) {
        Optional<Course> courseOptional = courseRepository.findByIdAndIsDeletedFalse(courseId);
        if (courseOptional.isEmpty()) {
            return ReviewSyncResult.skippedCourseNotFound();
        }

        Course course = courseOptional.get();
        Map<String, CourseRecommendationDocument> existingDocumentsBySourceKey =
                courseRecommendationDocumentRepository
                        .findByCourse_IdAndSourceTypeOrderByCreatedAtDesc(courseId, COURSE_REVIEW_SOURCE_TYPE)
                        .stream()
                        .collect(Collectors.toMap(
                                CourseRecommendationDocument::getSourceKey,
                                document -> document,
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ));

        ReviewSyncStats stats = new ReviewSyncStats();
        java.util.Set<String> activeReviewSourceKeys = new java.util.LinkedHashSet<>();
        for (CourseReview review : courseReviewRepository.findAllByCourse_IdOrderByCreatedAtDesc(courseId)) {
            if (!StringUtils.hasText(review.getContent())) {
                stats.skippedCount++;
                continue;
            }
            activeReviewSourceKeys.add(syncReviewDocument(course, review, existingDocumentsBySourceKey, stats));
        }

        for (CourseRecommendationDocument existingDocument : existingDocumentsBySourceKey.values()) {
            if (!activeReviewSourceKeys.contains(existingDocument.getSourceKey())
                    && !Boolean.TRUE.equals(existingDocument.getIsDeleted())) {
                existingDocument.softDelete();
                stats.deletedCount++;
            }
        }

        return new ReviewSyncResult(
                stats.createdCount,
                stats.updatedCount,
                stats.deletedCount,
                stats.skippedCount
        );
    }

    private String syncReviewDocument(
            Course course,
            CourseReview review,
            Map<String, CourseRecommendationDocument> existingDocumentsBySourceKey,
            ReviewSyncStats stats
    ) {
        String sourceKey = reviewSourceKey(review.getId());
        String title = reviewTitle(course);
        String content = reviewContent(review);
        ObjectNode metadata = buildReviewMetadata(course, review);
        CourseRecommendationDocument existingDocument = existingDocumentsBySourceKey.get(sourceKey);
        if (existingDocument == null) {
            courseRecommendationDocumentRepository.save(CourseRecommendationDocument.create(
                    course,
                    COURSE_REVIEW_SOURCE_TYPE,
                    sourceKey,
                    title,
                    content,
                    metadata
            ));
            stats.createdCount++;
            return sourceKey;
        }

        if (existingDocument.hasSameActiveContent(title, content, metadata)) {
            stats.skippedCount++;
            return sourceKey;
        }

        existingDocument.updateContent(title, content, metadata);
        stats.updatedCount++;
        return sourceKey;
    }

    private ObjectNode buildCourseMetadata(Course course) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("courseId", course.getId().toString());
        metadata.put("courseName", course.getName());
        metadata.put("type", course.getCourseType().name());
        ArrayNode themeCodes = metadata.putArray("themeCodes");
        for (CourseTheme courseTheme : courseThemeRepository.findAllByCourse_IdIn(List.of(course.getId()))) {
            themeCodes.add(courseTheme.getTheme().getCode());
        }
        return metadata;
    }

    private ObjectNode buildReviewMetadata(Course course, CourseReview review) {
        ObjectNode metadata = buildCourseMetadata(course);
        metadata.put("reviewId", review.getId().toString());
        metadata.put("source", COURSE_REVIEW_SOURCE_TYPE.name());
        metadata.put("rating", review.getRating());
        return metadata;
    }

    private String reviewSourceKey(UUID reviewId) {
        return "course-review-" + reviewId;
    }

    private String reviewTitle(Course course) {
        return course.getName() + " \uD6C4\uAE30";
    }

    private String reviewContent(CourseReview review) {
        return "\uD3C9\uC810 " + review.getRating() + "\uC810: " + review.getContent().trim();
    }

    public enum SyncResult {
        CREATED,
        UPDATED,
        DELETED_EMPTY_CONTENT,
        SKIPPED_EMPTY_CONTENT,
        SKIPPED_UNCHANGED,
        SKIPPED_COURSE_NOT_FOUND
    }

    public record ReviewSyncResult(
            int createdCount,
            int updatedCount,
            int deletedCount,
            int skippedCount
    ) {
        private static ReviewSyncResult skippedCourseNotFound() {
            return new ReviewSyncResult(0, 0, 0, 1);
        }
    }

    private static class ReviewSyncStats {
        private int createdCount;
        private int updatedCount;
        private int deletedCount;
        private int skippedCount;
    }
}
