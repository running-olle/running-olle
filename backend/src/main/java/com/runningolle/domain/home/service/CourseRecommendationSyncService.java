package com.runningolle.domain.home.service;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.home.dto.CourseRecommendationSyncResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseRecommendationSyncService {

    private final CourseRepository courseRepository;
    private final CourseRecommendationDocumentService courseRecommendationDocumentService;
    private final CourseRecommendationEmbeddingService courseRecommendationEmbeddingService;

    public CourseRecommendationSyncResponse syncPublicCourseRecommendations() {
        List<Course> courses = courseRepository.findAllByIsDeletedFalseAndIsPublicTrueOrderByCreatedAtDesc();
        SyncStats stats = new SyncStats();
        LocalDateTime syncedAt = LocalDateTime.now();

        for (Course course : courses) {
            stats.targetCourseCount++;
            accumulate(courseRecommendationDocumentService.syncCourseDescriptionDocument(course.getId()), stats);
            accumulate(courseRecommendationDocumentService.syncCourseReviewDocuments(course.getId()), stats);
            for (CourseRecommendationEmbeddingService.SyncResult embeddingResult :
                    courseRecommendationEmbeddingService.syncCourseRecommendationEmbeddings(course.getId())) {
                accumulate(embeddingResult, stats);
            }
        }

        return new CourseRecommendationSyncResponse(
                stats.targetCourseCount,
                stats.documentCreatedCount,
                stats.documentUpdatedCount,
                stats.documentDeletedCount,
                stats.documentSkippedCount,
                stats.embeddingSyncedCount,
                stats.embeddingDeletedCount,
                stats.embeddingSkippedCount,
                stats.embeddingFailedCount,
                syncedAt
        );
    }

    private void accumulate(CourseRecommendationDocumentService.SyncResult result, SyncStats stats) {
        switch (result) {
            case CREATED -> stats.documentCreatedCount++;
            case UPDATED -> stats.documentUpdatedCount++;
            case DELETED_EMPTY_CONTENT -> stats.documentDeletedCount++;
            case SKIPPED_EMPTY_CONTENT, SKIPPED_COURSE_NOT_FOUND -> stats.documentSkippedCount++;
        }
    }

    private void accumulate(CourseRecommendationDocumentService.ReviewSyncResult result, SyncStats stats) {
        stats.documentCreatedCount += result.createdCount();
        stats.documentUpdatedCount += result.updatedCount();
        stats.documentDeletedCount += result.deletedCount();
        stats.documentSkippedCount += result.skippedCount();
    }

    private void accumulate(CourseRecommendationEmbeddingService.SyncResult result, SyncStats stats) {
        switch (result) {
            case SYNCED -> stats.embeddingSyncedCount++;
            case DELETED_FROM_VECTOR_STORE -> stats.embeddingDeletedCount++;
            case SKIPPED_EMBEDDING_SYNC_DISABLED, SKIPPED_VECTOR_STORE_UNAVAILABLE, SKIPPED_DOCUMENT_NOT_FOUND ->
                    stats.embeddingSkippedCount++;
            case FAILED -> stats.embeddingFailedCount++;
        }
    }

    private static class SyncStats {
        private int targetCourseCount;
        private int documentCreatedCount;
        private int documentUpdatedCount;
        private int documentDeletedCount;
        private int documentSkippedCount;
        private int embeddingSyncedCount;
        private int embeddingDeletedCount;
        private int embeddingSkippedCount;
        private int embeddingFailedCount;
    }
}
