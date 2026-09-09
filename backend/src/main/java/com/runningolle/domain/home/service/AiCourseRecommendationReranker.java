package com.runningolle.domain.home.service;

import com.runningolle.domain.home.service.CourseRecommendationContextRetriever.RetrievedRecommendationContext;
import com.runningolle.domain.home.service.CourseRecommendationReranker.BaseRecommendationCandidate;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RagScoreBreakdown;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RecommendationUserPreference;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RerankedRecommendation;
import com.runningolle.domain.home.config.HomeRecommendationProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ChatClient.Builder.class)
@ConditionalOnProperty(prefix = "home.recommendation", name = "ai-enabled", havingValue = "true")
/**
 * Current AI phase: rerank only the already-selected base candidates using stored recommendation contexts.
 * This is not a vector-search retriever and must remain bounded to the provided candidate set.
 */
public class AiCourseRecommendationReranker implements CourseRecommendationReranker {

    private final ChatClient.Builder chatClientBuilder;
    private final CourseRecommendationContextRetriever courseRecommendationContextRetriever;
    private final HomeRecommendationProperties homeRecommendationProperties;

    @Override
    public List<RerankedRecommendation> rerank(
            RecommendationUserPreference preference,
            List<BaseRecommendationCandidate> candidates
    ) {
        Map<java.util.UUID, List<RetrievedRecommendationContext>> contextByCourseId = contextsByCourseId(
                courseRecommendationContextRetriever.retrieve(preference, candidates)
        );
        List<BaseRecommendationCandidate> candidatesWithContext = candidates.stream()
                .filter(candidate -> contextByCourseId.containsKey(candidate.courseId()))
                .toList();

        if (candidatesWithContext.isEmpty()) {
            log.info("Skipping AI reranking because no candidate has retrievable context. userId={}", preference.userId());
            return List.of();
        }

        BeanOutputConverter<List<AiRerankItem>> outputConverter =
                new BeanOutputConverter<>(new ParameterizedTypeReference<List<AiRerankItem>>() {});
        List<AiRerankItem> response = requestAiRerank(
                preference,
                candidatesWithContext,
                contextByCourseId,
                outputConverter
        );

        if (response == null || response.isEmpty()) {
            return List.of();
        }

        return response.stream()
                .map(AiRerankItem::toRerankedRecommendation)
                .toList();
    }

    private List<AiRerankItem> requestAiRerank(
            RecommendationUserPreference preference,
            List<BaseRecommendationCandidate> candidatesWithContext,
            Map<java.util.UUID, List<RetrievedRecommendationContext>> contextByCourseId,
            BeanOutputConverter<List<AiRerankItem>> outputConverter
    ) {
        CompletableFuture<List<AiRerankItem>> request = CompletableFuture.supplyAsync(() -> {
            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                    .system(systemPrompt())
                    .user(buildUserPrompt(
                            preference,
                            candidatesWithContext,
                            contextByCourseId,
                            outputConverter.getFormat()
                    ))
                    .call()
                    .entity(outputConverter);
        });

        try {
            return request.get(homeRecommendationProperties.getAi().getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            request.cancel(true);
            throw new IllegalStateException("AI recommendation reranking timed out.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI recommendation reranking was interrupted.", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("AI recommendation reranking failed.", cause);
        }
    }

    private String systemPrompt() {
        return """
                You rerank Jeju running course candidates for the Running Olle home screen.
                Use only the provided candidate descriptions, course reviews, and metadata.
                Do not invent missing structured facts such as distance, rating, or location.
                Evaluate only these six RAG criteria with the exact max scores:
                - userTypeFit: 20
                - themeFit: 25
                - reviewSatisfaction: 20
                - seasonFit: 15
                - safety: 10
                - convenience: 10
                Return only candidate courseIds that are provided.
                ragScore must equal the sum of the six criteria.
                recommendationReason must be written in Korean.
                recommendationReason should be concise.
                """;
    }

    private String buildUserPrompt(
            RecommendationUserPreference preference,
            List<BaseRecommendationCandidate> candidates,
            Map<java.util.UUID, List<RetrievedRecommendationContext>> contextByCourseId,
            String outputFormat
    ) {
        return """
                User preference:
                - userId: %s
                - preferredDistance: %s
                - preferredDifficulty: %s
                - userTypes: %s
                - themeCodes: %s

                Candidate courses:
                %s

                Output format:
                %s
                """.formatted(
                preference.userId(),
                preference.preferredDistance(),
                preference.preferredDifficulty(),
                preference.userTypes(),
                preference.themeCodes(),
                candidates.stream()
                        .map(candidate -> formatCandidate(candidate, contextByCourseId.getOrDefault(candidate.courseId(), List.of())))
                        .collect(Collectors.joining("\n")),
                outputFormat
        );
    }

    private String formatCandidate(
            BaseRecommendationCandidate candidate,
            List<RetrievedRecommendationContext> contexts
    ) {
        return """
                - courseId: %s
                  courseName: %s
                  courseType: %s
                  difficulty: %s
                  themes: %s
                  retrievedContext:
                %s
                """.formatted(
                candidate.courseId(),
                candidate.courseName(),
                candidate.courseType(),
                candidate.difficulty(),
                candidate.themeCodes(),
                formatContexts(contexts)
        );
    }

    private String formatContexts(List<RetrievedRecommendationContext> contexts) {
        return contexts.stream()
                .map(context -> "    - sourceType: " + context.sourceType() + "\n      content: " + sanitize(context.content()))
                .collect(Collectors.joining("\n"));
    }

    private Map<java.util.UUID, List<RetrievedRecommendationContext>> contextsByCourseId(
            List<RetrievedRecommendationContext> contexts
    ) {
        Map<java.util.UUID, List<RetrievedRecommendationContext>> contextByCourseId = new LinkedHashMap<>();
        for (RetrievedRecommendationContext context : contexts) {
            if (!StringUtils.hasText(context.content())) {
                continue;
            }
            contextByCourseId.computeIfAbsent(context.courseId(), ignored -> new java.util.ArrayList<>())
                    .add(new RetrievedRecommendationContext(
                            context.documentId(),
                            context.courseId(),
                            context.sourceType(),
                            context.content().trim()
                    ));
        }
        return contextByCourseId;
    }

    private String sanitize(String text) {
        return text.replace("\r", " ").replace("\n", " ");
    }

    private record AiRerankItem(
            java.util.UUID courseId,
            AiRagScores scores,
            double ragScore,
            String recommendationReason
    ) {
        private RerankedRecommendation toRerankedRecommendation() {
            return new RerankedRecommendation(
                    courseId,
                    new RagScoreBreakdown(
                            scores.userTypeFit(),
                            scores.themeFit(),
                            scores.reviewSatisfaction(),
                            scores.seasonFit(),
                            scores.safety(),
                            scores.convenience()
                    ),
                    ragScore,
                    recommendationReason
            );
        }
    }

    private record AiRagScores(
            double userTypeFit,
            double themeFit,
            double reviewSatisfaction,
            double seasonFit,
            double safety,
            double convenience
    ) {
    }
}
