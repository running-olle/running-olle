package com.runningolle.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.home.dto.CourseRecommendationSyncResponse;
import com.runningolle.domain.user.entity.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseRecommendationSyncServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseRecommendationDocumentService courseRecommendationDocumentService;

    @Mock
    private CourseRecommendationEmbeddingService courseRecommendationEmbeddingService;

    @Test
    void syncsAllPublicCoursesAndAggregatesResults() {
        Course courseA = course(UUID.randomUUID(), "A");
        Course courseB = course(UUID.randomUUID(), "B");

        CourseRecommendationSyncService service = new CourseRecommendationSyncService(
                courseRepository,
                courseRecommendationDocumentService,
                courseRecommendationEmbeddingService
        );

        given(courseRepository.findAllByIsDeletedFalseAndIsPublicTrueOrderByCreatedAtDesc())
                .willReturn(List.of(courseA, courseB));
        given(courseRecommendationDocumentService.syncCourseDescriptionDocument(courseA.getId()))
                .willReturn(CourseRecommendationDocumentService.SyncResult.CREATED);
        given(courseRecommendationDocumentService.syncCourseDescriptionDocument(courseB.getId()))
                .willReturn(CourseRecommendationDocumentService.SyncResult.SKIPPED_EMPTY_CONTENT);
        given(courseRecommendationDocumentService.syncCourseReviewDocuments(courseA.getId()))
                .willReturn(new CourseRecommendationDocumentService.ReviewSyncResult(1, 0, 0, 0));
        given(courseRecommendationDocumentService.syncCourseReviewDocuments(courseB.getId()))
                .willReturn(new CourseRecommendationDocumentService.ReviewSyncResult(0, 1, 0, 1));
        given(courseRecommendationEmbeddingService.syncCourseRecommendationEmbeddings(courseA.getId()))
                .willReturn(List.of(
                        CourseRecommendationEmbeddingService.SyncResult.SYNCED,
                        CourseRecommendationEmbeddingService.SyncResult.SYNCED
                ));
        given(courseRecommendationEmbeddingService.syncCourseRecommendationEmbeddings(courseB.getId()))
                .willReturn(List.of(CourseRecommendationEmbeddingService.SyncResult.FAILED));

        CourseRecommendationSyncResponse response = service.syncPublicCourseRecommendations();

        assertThat(response.targetCourseCount()).isEqualTo(2);
        assertThat(response.documentCreatedCount()).isEqualTo(2);
        assertThat(response.documentUpdatedCount()).isEqualTo(1);
        assertThat(response.documentSkippedCount()).isEqualTo(2);
        assertThat(response.embeddingSyncedCount()).isEqualTo(2);
        assertThat(response.embeddingFailedCount()).isEqualTo(1);
        assertThat(response.syncedAt()).isNotNull();
        verify(courseRecommendationDocumentService).syncCourseDescriptionDocument(courseA.getId());
        verify(courseRecommendationDocumentService).syncCourseDescriptionDocument(courseB.getId());
        verify(courseRecommendationDocumentService).syncCourseReviewDocuments(courseA.getId());
        verify(courseRecommendationDocumentService).syncCourseReviewDocuments(courseB.getId());
        verify(courseRecommendationEmbeddingService).syncCourseRecommendationEmbeddings(courseA.getId());
        verify(courseRecommendationEmbeddingService).syncCourseRecommendationEmbeddings(courseB.getId());
    }

    private Course course(UUID courseId, String name) {
        User creator = User.createKakaoUser("creator-" + courseId);
        ReflectionTestUtils.setField(creator, "id", UUID.randomUUID());
        var route = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(126.5, 33.5),
                new Coordinate(126.51, 33.51)
        });
        route.setSRID(4326);
        var startPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(126.5, 33.5));
        startPoint.setSRID(4326);
        Course course = Course.create(
                creator,
                name,
                "description",
                CourseType.RUNNING_COURSE,
                new BigDecimal("5.0"),
                50,
                new BigDecimal("20.0"),
                Difficulty.MID,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                route,
                startPoint,
                null,
                true
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }
}
