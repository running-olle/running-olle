package com.runningolle.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.service.CourseRecommendationContextRetriever.RetrievedRecommendationContext;
import com.runningolle.domain.home.service.CourseRecommendationReranker.BaseRecommendationCandidate;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RecommendationUserPreference;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RerankedRecommendation;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import com.runningolle.domain.user.enums.UserTypeCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.StructuredOutputConverter;

@ExtendWith(MockitoExtension.class)
class AiCourseRecommendationRerankerTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private CourseRecommendationContextRetriever courseRecommendationContextRetriever;

    private HomeRecommendationProperties homeRecommendationProperties;
    private AiCourseRecommendationReranker reranker;

    @BeforeEach
    void setUp() {
        homeRecommendationProperties = new HomeRecommendationProperties();
        reranker = new AiCourseRecommendationReranker(
                chatClientBuilder,
                courseRecommendationContextRetriever,
                homeRecommendationProperties
        );
    }

    @Test
    void skipsAiCallWhenNoCandidateHasDescription() {
        given(courseRecommendationContextRetriever.retrieve(any(), any()))
                .willReturn(List.of());

        List<RerankedRecommendation> response = reranker.rerank(
                preference(),
                List.of(
                        candidate("A", null),
                        candidate("B", " "),
                        candidate("C", "")
                )
        );

        assertThat(response).isEmpty();
        verify(chatClientBuilder, never()).build();
    }

    @Test
    void excludesCandidatesWithoutDescriptionFromPrompt() {
        BaseRecommendationCandidate describedCandidate = candidate("Described", "Ocean view and photo spot");
        BaseRecommendationCandidate undescribedCandidate = candidate("Undescribed", null);

        given(courseRecommendationContextRetriever.retrieve(any(), any()))
                .willReturn(List.of(
                        new RetrievedRecommendationContext(
                                "doc-1",
                                describedCandidate.courseId(),
                                "COURSE_DESCRIPTION",
                                "Ocean view and photo spot"
                        )
                ));
        given(chatClientBuilder.build()).willReturn(chatClient);
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.system(any(String.class))).willReturn(requestSpec);
        given(requestSpec.user(any(String.class))).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
        given(callResponseSpec.entity(any(StructuredOutputConverter.class))).willReturn(List.of());

        reranker.rerank(
                preference(),
                List.of(
                        describedCandidate,
                        undescribedCandidate
                )
        );

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(userPromptCaptor.capture());
        assertThat(userPromptCaptor.getValue()).contains("Described");
        assertThat(userPromptCaptor.getValue()).contains("retrievedContext");
        assertThat(userPromptCaptor.getValue()).contains("COURSE_DESCRIPTION");
        assertThat(userPromptCaptor.getValue()).doesNotContain("Undescribed");
    }

    @Test
    void returnsEmptyWhenAiReturnsNoStructuredResult() {
        BaseRecommendationCandidate candidate = candidate("A", "Forest path with shade");

        given(courseRecommendationContextRetriever.retrieve(any(), any()))
                .willReturn(List.of(
                        new RetrievedRecommendationContext(
                                "doc-1",
                                candidate.courseId(),
                                "COURSE_DESCRIPTION",
                                "Forest path with shade"
                        )
                ));
        given(chatClientBuilder.build()).willReturn(chatClient);
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.system(any(String.class))).willReturn(requestSpec);
        given(requestSpec.user(any(String.class))).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
        given(callResponseSpec.entity(any(StructuredOutputConverter.class))).willReturn(null);

        List<RerankedRecommendation> response = reranker.rerank(
                preference(),
                List.of(candidate)
        );

        assertThat(response).isEmpty();
    }

    @Test
    void failsFastWhenAiCallExceedsConfiguredTimeout() {
        BaseRecommendationCandidate candidate = candidate("A", "Forest path with shade");
        homeRecommendationProperties.getAi().setRequestTimeoutMs(100);

        given(courseRecommendationContextRetriever.retrieve(any(), any()))
                .willReturn(List.of(
                        new RetrievedRecommendationContext(
                                "doc-1",
                                candidate.courseId(),
                                "COURSE_DESCRIPTION",
                                "Forest path with shade"
                        )
                ));
        given(chatClientBuilder.build()).willReturn(chatClient);
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.system(any(String.class))).willReturn(requestSpec);
        given(requestSpec.user(any(String.class))).willReturn(requestSpec);
        given(requestSpec.call()).willAnswer(invocation -> {
            Thread.sleep(300);
            return callResponseSpec;
        });

        assertThatThrownBy(() -> reranker.rerank(preference(), List.of(candidate)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timed out");
    }

    private RecommendationUserPreference preference() {
        return new RecommendationUserPreference(
                UUID.randomUUID(),
                PreferredDistance.FROM_5_TO_10KM,
                PreferredDifficulty.NORMAL,
                Set.of(UserTypeCode.RELAXED_TRAVELER),
                Set.of("COAST", "PHOTO")
        );
    }

    private BaseRecommendationCandidate candidate(String courseName, String description) {
        return new BaseRecommendationCandidate(
                UUID.randomUUID(),
                courseName,
                description,
                CourseType.RUNNING_COURSE,
                new BigDecimal("6.0"),
                Difficulty.LOW,
                new BigDecimal("4.6"),
                42,
                List.of("COAST"),
                3.0,
                82.0
        );
    }
}
