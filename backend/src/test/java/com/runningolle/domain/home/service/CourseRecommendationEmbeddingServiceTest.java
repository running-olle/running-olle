package com.runningolle.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.entity.CourseRecommendationDocument;
import com.runningolle.domain.home.entity.RecommendationDocumentEmbeddingStatus;
import com.runningolle.domain.home.entity.RecommendationDocumentSourceType;
import com.runningolle.domain.home.repository.CourseRecommendationDocumentRepository;
import com.runningolle.domain.user.entity.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseRecommendationEmbeddingServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private CourseRecommendationDocumentRepository courseRecommendationDocumentRepository;

    @Mock
    private ObjectProvider<VectorStore> vectorStoreProvider;

    @Mock
    private VectorStore vectorStore;

    @Test
    void skipsWhenEmbeddingSyncIsDisabled() {
        HomeRecommendationProperties properties = new HomeRecommendationProperties();
        CourseRecommendationEmbeddingService service = new CourseRecommendationEmbeddingService(
                courseRecommendationDocumentRepository,
                properties,
                vectorStoreProvider
        );

        CourseRecommendationEmbeddingService.SyncResult result =
                service.syncCourseDescriptionEmbedding(UUID.randomUUID());

        assertThat(result).isEqualTo(CourseRecommendationEmbeddingService.SyncResult.SKIPPED_EMBEDDING_SYNC_DISABLED);
        verify(vectorStoreProvider, never()).getIfAvailable();
    }

    @Test
    void syncsActiveDocumentToVectorStore() {
        UUID courseId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        HomeRecommendationProperties properties = new HomeRecommendationProperties();
        properties.setEmbeddingSyncEnabled(true);
        CourseRecommendationEmbeddingService service = new CourseRecommendationEmbeddingService(
                courseRecommendationDocumentRepository,
                properties,
                vectorStoreProvider
        );
        ReflectionTestUtils.setField(service, "embeddingModel", "openai");

        CourseRecommendationDocument document = document(courseId, documentId, "Coast route", false);

        given(vectorStoreProvider.getIfAvailable()).willReturn(vectorStore);
        given(courseRecommendationDocumentRepository.findByCourse_IdAndSourceTypeAndSourceKey(
                courseId,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description"
        )).willReturn(Optional.of(document));

        CourseRecommendationEmbeddingService.SyncResult result = service.syncCourseDescriptionEmbedding(courseId);

        assertThat(result).isEqualTo(CourseRecommendationEmbeddingService.SyncResult.SYNCED);
        verify(vectorStore).delete(List.of(documentId.toString()));
        verify(vectorStore).add(anyList());
        assertThat(document.getEmbeddingStatus()).isEqualTo(RecommendationDocumentEmbeddingStatus.COMPLETED);
        assertThat(document.getEmbeddingModel()).isEqualTo("openai");
        assertThat(document.getEmbeddedAt()).isNotNull();
    }

    @Test
    void deletesVectorDocumentWhenRecommendationDocumentWasSoftDeleted() {
        UUID courseId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        HomeRecommendationProperties properties = new HomeRecommendationProperties();
        properties.setEmbeddingSyncEnabled(true);
        CourseRecommendationEmbeddingService service = new CourseRecommendationEmbeddingService(
                courseRecommendationDocumentRepository,
                properties,
                vectorStoreProvider
        );

        CourseRecommendationDocument document = document(courseId, documentId, "Old coast route", true);

        given(vectorStoreProvider.getIfAvailable()).willReturn(vectorStore);
        given(courseRecommendationDocumentRepository.findByCourse_IdAndSourceTypeAndSourceKey(
                courseId,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description"
        )).willReturn(Optional.of(document));

        CourseRecommendationEmbeddingService.SyncResult result = service.syncCourseDescriptionEmbedding(courseId);

        assertThat(result).isEqualTo(CourseRecommendationEmbeddingService.SyncResult.DELETED_FROM_VECTOR_STORE);
        verify(vectorStore).delete(List.of(documentId.toString()));
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void marksFailureWhenVectorStoreAddFails() {
        UUID courseId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        HomeRecommendationProperties properties = new HomeRecommendationProperties();
        properties.setEmbeddingSyncEnabled(true);
        CourseRecommendationEmbeddingService service = new CourseRecommendationEmbeddingService(
                courseRecommendationDocumentRepository,
                properties,
                vectorStoreProvider
        );

        CourseRecommendationDocument document = document(courseId, documentId, "Forest route", false);

        given(vectorStoreProvider.getIfAvailable()).willReturn(vectorStore);
        given(courseRecommendationDocumentRepository.findByCourse_IdAndSourceTypeAndSourceKey(
                courseId,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description"
        )).willReturn(Optional.of(document));
        org.mockito.Mockito.doThrow(new IllegalStateException("vector add failed"))
                .when(vectorStore)
                .add(anyList());

        CourseRecommendationEmbeddingService.SyncResult result = service.syncCourseDescriptionEmbedding(courseId);

        assertThat(result).isEqualTo(CourseRecommendationEmbeddingService.SyncResult.FAILED);
        assertThat(document.getEmbeddingStatus()).isEqualTo(RecommendationDocumentEmbeddingStatus.FAILED);
        assertThat(document.getEmbeddingFailureReason()).contains("vector add failed");
    }

    @Test
    void storesMetadataInVectorDocument() {
        UUID courseId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        HomeRecommendationProperties properties = new HomeRecommendationProperties();
        properties.setEmbeddingSyncEnabled(true);
        CourseRecommendationEmbeddingService service = new CourseRecommendationEmbeddingService(
                courseRecommendationDocumentRepository,
                properties,
                vectorStoreProvider
        );

        CourseRecommendationDocument document = document(courseId, documentId, "Photo spot route", false);

        given(vectorStoreProvider.getIfAvailable()).willReturn(vectorStore);
        given(courseRecommendationDocumentRepository.findByCourse_IdAndSourceTypeAndSourceKey(
                courseId,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description"
        )).willReturn(Optional.of(document));

        service.syncCourseDescriptionEmbedding(courseId);

        org.mockito.ArgumentCaptor<List> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        Document stored = (Document) captor.getValue().get(0);
        assertThat(String.valueOf(stored.getMetadata().get("courseId"))).isEqualTo(courseId.toString());
        assertThat(String.valueOf(stored.getMetadata().get("courseName"))).isEqualTo("Course-" + courseId);
        assertThat(String.valueOf(stored.getMetadata().get("type"))).isEqualTo("RUNNING_COURSE");
        assertThat(stored.getMetadata()).containsKey("themeCodes");
        assertThat((List<Object>) stored.getMetadata().get("themeCodes")).containsExactly("COAST", "PHOTO");
    }

    private CourseRecommendationDocument document(UUID courseId, UUID documentId, String content, boolean deleted) {
        Course course = course(courseId);
        com.fasterxml.jackson.databind.node.ObjectNode metadata = OBJECT_MAPPER.createObjectNode();
        metadata.put("courseId", courseId.toString());
        metadata.put("courseName", course.getName());
        metadata.put("type", course.getCourseType().name());
        metadata.putArray("themeCodes")
                .add("COAST")
                .add("PHOTO");
        CourseRecommendationDocument document = CourseRecommendationDocument.create(
                course,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course-description",
                course.getName(),
                content,
                metadata
        );
        ReflectionTestUtils.setField(document, "id", documentId);
        if (deleted) {
            document.softDelete();
        }
        return document;
    }

    private Course course(UUID courseId) {
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
                "Course-" + courseId,
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
