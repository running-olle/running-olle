package com.runningolle.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseReview;
import com.runningolle.domain.course.entity.CourseTheme;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseReviewRepository;
import com.runningolle.domain.course.repository.CourseThemeRepository;
import com.runningolle.domain.home.entity.CourseRecommendationDocument;
import com.runningolle.domain.home.entity.RecommendationDocumentSourceType;
import com.runningolle.domain.home.repository.CourseRecommendationDocumentRepository;
import com.runningolle.domain.user.entity.Theme;
import com.runningolle.domain.user.entity.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseRecommendationDocumentServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseThemeRepository courseThemeRepository;

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CourseRecommendationDocumentRepository courseRecommendationDocumentRepository;

    @Mock
    private CourseReview courseReview;

    @Test
    void createsCourseDescriptionDocumentWhenMissing() {
        UUID courseId = UUID.randomUUID();
        Course course = course(courseId, "Aewol Coast", " Coast description ");
        Theme coast = Theme.create("COAST", "Coast");
        Theme photo = Theme.create("PHOTO", "Photo");
        CourseRecommendationDocumentService service = new CourseRecommendationDocumentService(
                courseRepository,
                courseReviewRepository,
                courseThemeRepository,
                courseRecommendationDocumentRepository,
                new ObjectMapper()
        );

        given(courseRepository.findByIdAndIsDeletedFalse(courseId)).willReturn(Optional.of(course));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(courseId)))
                .willReturn(List.of(CourseTheme.of(course, coast), CourseTheme.of(course, photo)));
        given(courseRecommendationDocumentRepository.findByCourse_IdAndSourceTypeAndSourceKey(
                courseId,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description"
        )).willReturn(Optional.empty());

        CourseRecommendationDocumentService.SyncResult result = service.syncCourseDescriptionDocument(courseId);

        assertThat(result).isEqualTo(CourseRecommendationDocumentService.SyncResult.CREATED);
        ArgumentCaptor<CourseRecommendationDocument> captor = ArgumentCaptor.forClass(CourseRecommendationDocument.class);
        verify(courseRecommendationDocumentRepository).save(captor.capture());
        CourseRecommendationDocument saved = captor.getValue();
        assertThat(saved.getContent()).isEqualTo("Coast description");
        assertThat(saved.getMetadata().get("courseId").asText()).isEqualTo(courseId.toString());
        assertThat(saved.getMetadata().get("courseName").asText()).isEqualTo("Aewol Coast");
        assertThat(saved.getMetadata().get("type").asText()).isEqualTo("RUNNING_COURSE");
        assertThat(saved.getMetadata().get("themeCodes")).extracting(node -> node.asText())
                .containsExactly("COAST", "PHOTO");
    }

    @Test
    void updatesExistingDocumentWhenDescriptionExists() {
        UUID courseId = UUID.randomUUID();
        Course course = course(courseId, "Forest Loop", "Updated description");
        CourseRecommendationDocument existing = CourseRecommendationDocument.create(
                course,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description",
                "Old title",
                "Old content",
                new ObjectMapper().createObjectNode().put("courseId", courseId.toString())
        );
        CourseRecommendationDocumentService service = new CourseRecommendationDocumentService(
                courseRepository,
                courseReviewRepository,
                courseThemeRepository,
                courseRecommendationDocumentRepository,
                new ObjectMapper()
        );

        given(courseRepository.findByIdAndIsDeletedFalse(courseId)).willReturn(Optional.of(course));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(courseId))).willReturn(List.of());
        given(courseRecommendationDocumentRepository.findByCourse_IdAndSourceTypeAndSourceKey(
                courseId,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description"
        )).willReturn(Optional.of(existing));

        CourseRecommendationDocumentService.SyncResult result = service.syncCourseDescriptionDocument(courseId);

        assertThat(result).isEqualTo(CourseRecommendationDocumentService.SyncResult.UPDATED);
        verify(courseRecommendationDocumentRepository, never()).save(any());
        assertThat(existing.getContent()).isEqualTo("Updated description");
        assertThat(existing.getTitle()).isEqualTo("Forest Loop");
    }

    @Test
    void skipsExistingDescriptionDocumentWhenContentIsUnchanged() {
        UUID courseId = UUID.randomUUID();
        Course course = course(courseId, "Forest Loop", "Same description");
        ObjectMapper objectMapper = new ObjectMapper();
        CourseRecommendationDocument existing = CourseRecommendationDocument.create(
                course,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description",
                "Forest Loop",
                "Same description",
                objectMapper.createObjectNode()
                        .put("courseId", courseId.toString())
                        .put("courseName", "Forest Loop")
                        .put("type", "RUNNING_COURSE")
                        .set("themeCodes", objectMapper.createArrayNode())
        );
        CourseRecommendationDocumentService service = new CourseRecommendationDocumentService(
                courseRepository,
                courseReviewRepository,
                courseThemeRepository,
                courseRecommendationDocumentRepository,
                objectMapper
        );

        given(courseRepository.findByIdAndIsDeletedFalse(courseId)).willReturn(Optional.of(course));
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(courseId))).willReturn(List.of());
        given(courseRecommendationDocumentRepository.findByCourse_IdAndSourceTypeAndSourceKey(
                courseId,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description"
        )).willReturn(Optional.of(existing));

        CourseRecommendationDocumentService.SyncResult result = service.syncCourseDescriptionDocument(courseId);

        assertThat(result).isEqualTo(CourseRecommendationDocumentService.SyncResult.SKIPPED_UNCHANGED);
        verify(courseRecommendationDocumentRepository, never()).save(any());
    }

    @Test
    void softDeletesExistingDocumentWhenDescriptionBecomesEmpty() {
        UUID courseId = UUID.randomUUID();
        Course course = course(courseId, "Oreum Run", " ");
        CourseRecommendationDocument existing = CourseRecommendationDocument.create(
                course,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description",
                "Oreum Run",
                "Old content",
                new ObjectMapper().createObjectNode().put("courseId", courseId.toString())
        );
        CourseRecommendationDocumentService service = new CourseRecommendationDocumentService(
                courseRepository,
                courseReviewRepository,
                courseThemeRepository,
                courseRecommendationDocumentRepository,
                new ObjectMapper()
        );

        given(courseRepository.findByIdAndIsDeletedFalse(courseId)).willReturn(Optional.of(course));
        given(courseRecommendationDocumentRepository.findByCourse_IdAndSourceTypeAndSourceKey(
                courseId,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description"
        )).willReturn(Optional.of(existing));

        CourseRecommendationDocumentService.SyncResult result = service.syncCourseDescriptionDocument(courseId);

        assertThat(result).isEqualTo(CourseRecommendationDocumentService.SyncResult.DELETED_EMPTY_CONTENT);
        assertThat(existing.getIsDeleted()).isTrue();
    }

    @Test
    void skipsWhenCourseDoesNotExist() {
        UUID courseId = UUID.randomUUID();
        CourseRecommendationDocumentService service = new CourseRecommendationDocumentService(
                courseRepository,
                courseReviewRepository,
                courseThemeRepository,
                courseRecommendationDocumentRepository,
                new ObjectMapper()
        );

        given(courseRepository.findByIdAndIsDeletedFalse(courseId)).willReturn(Optional.empty());

        CourseRecommendationDocumentService.SyncResult result = service.syncCourseDescriptionDocument(courseId);

        assertThat(result).isEqualTo(CourseRecommendationDocumentService.SyncResult.SKIPPED_COURSE_NOT_FOUND);
    }

    @Test
    void createsCourseReviewDocumentWhenReviewHasContent() {
        UUID courseId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        Course course = course(courseId, "Aewol Coast", "Course description");
        Theme coast = Theme.create("COAST", "Coast");
        CourseRecommendationDocumentService service = new CourseRecommendationDocumentService(
                courseRepository,
                courseReviewRepository,
                courseThemeRepository,
                courseRecommendationDocumentRepository,
                new ObjectMapper()
        );

        given(courseRepository.findByIdAndIsDeletedFalse(courseId)).willReturn(Optional.of(course));
        given(courseRecommendationDocumentRepository.findByCourse_IdAndSourceTypeOrderByCreatedAtDesc(
                courseId,
                RecommendationDocumentSourceType.COURSE_REVIEW
        )).willReturn(List.of());
        given(courseReviewRepository.findAllByCourse_IdOrderByCreatedAtDesc(courseId)).willReturn(List.of(courseReview));
        given(courseReview.getId()).willReturn(reviewId);
        given(courseReview.getContent()).willReturn("바다 전망이 좋고 사진 찍기 좋은 코스였어요.");
        given(courseReview.getRating()).willReturn(5);
        given(courseThemeRepository.findAllByCourse_IdIn(List.of(courseId)))
                .willReturn(List.of(CourseTheme.of(course, coast)));

        CourseRecommendationDocumentService.ReviewSyncResult result = service.syncCourseReviewDocuments(courseId);

        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isZero();
        assertThat(result.deletedCount()).isZero();
        assertThat(result.skippedCount()).isZero();
        ArgumentCaptor<CourseRecommendationDocument> captor = ArgumentCaptor.forClass(CourseRecommendationDocument.class);
        verify(courseRecommendationDocumentRepository).save(captor.capture());
        CourseRecommendationDocument saved = captor.getValue();
        assertThat(saved.getSourceType()).isEqualTo(RecommendationDocumentSourceType.COURSE_REVIEW);
        assertThat(saved.getSourceKey()).isEqualTo("course-review-" + reviewId);
        assertThat(saved.getContent()).isEqualTo("평점 5점: 바다 전망이 좋고 사진 찍기 좋은 코스였어요.");
        assertThat(saved.getMetadata().get("reviewId").asText()).isEqualTo(reviewId.toString());
        assertThat(saved.getMetadata().get("rating").asInt()).isEqualTo(5);
        assertThat(saved.getMetadata().get("themeCodes")).extracting(node -> node.asText())
                .containsExactly("COAST");
    }

    private Course course(UUID courseId, String name, String description) {
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
                description,
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
