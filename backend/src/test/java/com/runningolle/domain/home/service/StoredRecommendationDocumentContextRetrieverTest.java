package com.runningolle.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.entity.CourseRecommendationDocument;
import com.runningolle.domain.home.entity.RecommendationDocumentSourceType;
import com.runningolle.domain.home.repository.CourseRecommendationDocumentRepository;
import com.runningolle.domain.home.service.CourseRecommendationContextRetriever.RetrievedRecommendationContext;
import com.runningolle.domain.home.service.CourseRecommendationReranker.BaseRecommendationCandidate;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RecommendationUserPreference;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import com.runningolle.domain.user.enums.UserTypeCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoredRecommendationDocumentContextRetrieverTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private CourseRecommendationDocumentRepository courseRecommendationDocumentRepository;

    @Mock
    private ObjectProvider<VectorStore> vectorStoreProvider;

    @Mock
    private VectorStore vectorStore;

    @Test
    void prefersStoredDocumentsAndFallsBackToCandidateDescriptions() {
        HomeRecommendationProperties properties = new HomeRecommendationProperties();
        properties.getRag().setCandidateDocumentLimit(3);
        StoredRecommendationDocumentContextRetriever retriever =
                new StoredRecommendationDocumentContextRetriever(
                        courseRecommendationDocumentRepository,
                        properties,
                        vectorStoreProvider
                );

        BaseRecommendationCandidate storedCandidate = candidate(UUID.randomUUID(), "Stored Course", "candidate description");
        BaseRecommendationCandidate fallbackCandidate = candidate(UUID.randomUUID(), "Fallback Course", "fallback description");
        BaseRecommendationCandidate emptyCandidate = candidate(UUID.randomUUID(), "Empty", null);

        given(courseRecommendationDocumentRepository.findByCourse_IdInAndIsDeletedFalseOrderByCourse_IdAscCreatedAtDesc(
                List.of(storedCandidate.courseId(), fallbackCandidate.courseId(), emptyCandidate.courseId())
        )).willReturn(List.of(
                document(storedCandidate.courseId(), "stored doc 1"),
                document(storedCandidate.courseId(), "stored doc 2")
        ));

        List<RetrievedRecommendationContext> contexts = retriever.retrieve(
                preference(),
                List.of(storedCandidate, fallbackCandidate, emptyCandidate)
        );

        assertThat(contexts).hasSize(3);
        assertThat(contexts)
                .extracting(RetrievedRecommendationContext::content)
                .containsExactly("stored doc 1", "stored doc 2", "fallback description");
        assertThat(contexts)
                .extracting(RetrievedRecommendationContext::sourceType)
                .containsExactly("COURSE_DESCRIPTION", "COURSE_DESCRIPTION", "COURSE_DESCRIPTION");
    }

    @Test
    void respectsConfiguredDocumentLimitAcrossStoredAndFallbackContexts() {
        HomeRecommendationProperties properties = new HomeRecommendationProperties();
        properties.getRag().setCandidateDocumentLimit(2);
        StoredRecommendationDocumentContextRetriever retriever =
                new StoredRecommendationDocumentContextRetriever(
                        courseRecommendationDocumentRepository,
                        properties,
                        vectorStoreProvider
                );

        BaseRecommendationCandidate candidateA = candidate(UUID.randomUUID(), "A", "desc A");
        BaseRecommendationCandidate candidateB = candidate(UUID.randomUUID(), "B", "desc B");

        given(courseRecommendationDocumentRepository.findByCourse_IdInAndIsDeletedFalseOrderByCourse_IdAscCreatedAtDesc(
                List.of(candidateA.courseId(), candidateB.courseId())
        )).willReturn(List.of(
                document(candidateA.courseId(), "stored A1"),
                document(candidateA.courseId(), "stored A2"),
                document(candidateB.courseId(), "stored B1")
        ));

        List<RetrievedRecommendationContext> contexts = retriever.retrieve(
                preference(),
                List.of(candidateA, candidateB)
        );

        assertThat(contexts).hasSize(2);
        assertThat(contexts)
                .extracting(RetrievedRecommendationContext::content)
                .containsExactly("stored A1", "stored A2");
    }

    @Test
    void usesVectorStoreWhenRagIsEnabled() {
        HomeRecommendationProperties properties = new HomeRecommendationProperties();
        properties.getRag().setEnabled(true);
        properties.getRag().setCandidateDocumentLimit(2);
        StoredRecommendationDocumentContextRetriever retriever =
                new StoredRecommendationDocumentContextRetriever(
                        courseRecommendationDocumentRepository,
                        properties,
                        vectorStoreProvider
                );

        BaseRecommendationCandidate candidateA = candidate(UUID.randomUUID(), "A", "coast view");
        BaseRecommendationCandidate candidateB = candidate(UUID.randomUUID(), "B", "forest path");

        given(vectorStoreProvider.getIfAvailable()).willReturn(vectorStore);
        given(courseRecommendationDocumentRepository.findByCourse_IdInAndIsDeletedFalseOrderByCourse_IdAscCreatedAtDesc(
                List.of(candidateA.courseId(), candidateB.courseId())
        )).willReturn(List.of());
        given(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .willReturn(List.of(
                        Document.builder()
                                .id("doc-b")
                                .text("forest path")
                                .metadata("courseId", candidateB.courseId().toString())
                                .metadata("sourceType", "COURSE_DESCRIPTION")
                                .build(),
                        Document.builder()
                                .id("doc-a")
                                .text("coast view")
                                .metadata("courseId", candidateA.courseId().toString())
                                .metadata("sourceType", "COURSE_DESCRIPTION")
                                .build()
                ));

        List<RetrievedRecommendationContext> contexts = retriever.retrieve(
                preference(),
                List.of(candidateA, candidateB)
        );

        assertThat(contexts)
                .extracting(RetrievedRecommendationContext::courseId)
                .containsExactly(candidateB.courseId(), candidateA.courseId());

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getTopK()).isEqualTo(2);
        assertThat(requestCaptor.getValue().hasFilterExpression()).isTrue();
        verify(vectorStore, never()).add(org.mockito.ArgumentMatchers.anyList());
        verify(vectorStore, never()).delete(org.mockito.ArgumentMatchers.anyList());
    }

    private CourseRecommendationDocument document(UUID courseId, String content) {
        Course course = course(courseId);
        CourseRecommendationDocument document = CourseRecommendationDocument.create(
                course,
                RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                "course:" + courseId,
                course.getName(),
                content,
                OBJECT_MAPPER.createObjectNode().put("courseId", courseId.toString())
        );
        ReflectionTestUtils.setField(document, "id", UUID.randomUUID());
        return document;
    }

    private RecommendationUserPreference preference() {
        return new RecommendationUserPreference(
                UUID.randomUUID(),
                PreferredDistance.FROM_5_TO_10KM,
                PreferredDifficulty.NORMAL,
                Set.of(UserTypeCode.RELAXED_TRAVELER),
                Set.of("COAST")
        );
    }

    private BaseRecommendationCandidate candidate(UUID courseId, String courseName, String description) {
        return new BaseRecommendationCandidate(
                courseId,
                courseName,
                description,
                CourseType.RUNNING_COURSE,
                new BigDecimal("6.0"),
                Difficulty.LOW,
                new BigDecimal("4.5"),
                10,
                List.of("COAST"),
                2.0,
                70.0
        );
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
                null,
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
