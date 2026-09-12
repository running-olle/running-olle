package com.runningolle.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.service.AiCourseRecommendationReranker;
import com.runningolle.domain.home.service.CourseRecommendationContextRetriever;
import com.runningolle.domain.home.service.CourseRecommendationContextRetriever.RetrievedRecommendationContext;
import com.runningolle.domain.home.service.CourseRecommendationReranker.BaseRecommendationCandidate;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RecommendationUserPreference;
import com.runningolle.domain.home.service.CourseRecommendationReranker.RerankedRecommendation;
import com.runningolle.domain.home.service.CourseRecommendationRerankValidator;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import com.runningolle.domain.user.enums.UserTypeCode;
import io.micrometer.observation.ObservationRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;

@EnabledIfSystemProperty(named = "runningolle.external-smoke", matches = "true")
class OpenAiRecommendationRerankerSmokeTest {

    @Test
    void openAiRerankerReturnsValidStructuredScoresForProvidedCandidatesOnly() {
        String apiKey = readSecret("OPENAI_API_KEY", "openAiApiKey", "spring.ai.openai.api-key");
        assumeTrue(StringUtils.hasText(apiKey), "OpenAI API key is required.");
        assumeTrue(!apiKey.startsWith("YOUR_"), "OpenAI API key placeholder is not usable.");

        UUID coastCourseId = UUID.randomUUID();
        UUID forestCourseId = UUID.randomUUID();
        List<BaseRecommendationCandidate> candidates = List.of(
                candidate(coastCourseId, "Aewol Ocean Photo Run", "COAST", "PHOTO"),
                candidate(forestCourseId, "Saryeoni Forest Shade Run", "FOREST")
        );
        RecommendationUserPreference preference = new RecommendationUserPreference(
                UUID.randomUUID(),
                PreferredDistance.FROM_5_TO_10KM,
                PreferredDifficulty.EASY,
                Set.of(UserTypeCode.RELAXED_TRAVELER),
                Set.of("COAST", "PHOTO")
        );

        AiCourseRecommendationReranker reranker = new AiCourseRecommendationReranker(
                chatClientBuilder(apiKey),
                fixedContextRetriever(),
                new HomeRecommendationProperties()
        );
        List<RerankedRecommendation> reranked = reranker.rerank(preference, candidates);
        Map<UUID, RerankedRecommendation> valid = new CourseRecommendationRerankValidator()
                .validate(candidates, reranked);

        assertThat(valid).isNotEmpty();
        assertThat(valid.keySet()).isSubsetOf(coastCourseId, forestCourseId);
        assertThat(valid.values())
                .allSatisfy(recommendation -> {
                    assertThat(recommendation.ragScore()).isBetween(0.0, 100.0);
                    assertThat(recommendation.recommendationReason()).isNotBlank();
                });
    }

    private ChatClient.Builder chatClientBuilder(String apiKey) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder().apiKey(apiKey).build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4o-mini")
                        .temperature(0.0)
                        .maxTokens(600)
                        .build())
                .toolCallingManager(ToolCallingManager.builder().build())
                .retryTemplate(RetryTemplate.defaultInstance())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
        return ChatClient.builder(chatModel);
    }

    private CourseRecommendationContextRetriever fixedContextRetriever() {
        return (preference, candidates) -> candidates.stream()
                .map(candidate -> new RetrievedRecommendationContext(
                        "smoke-doc-" + candidate.courseId(),
                        candidate.courseId(),
                        "COURSE_DESCRIPTION",
                        candidate.description()
                ))
                .toList();
    }

    private BaseRecommendationCandidate candidate(UUID courseId, String name, String... themeCodes) {
        String description = name + " has Jeju scenery, accessible running conditions, and clear travel appeal.";
        return new BaseRecommendationCandidate(
                courseId,
                name,
                description,
                CourseType.RUNNING_COURSE,
                new BigDecimal("6.00"),
                Difficulty.LOW,
                new BigDecimal("4.50"),
                30,
                List.of(themeCodes),
                2.0,
                80.0
        );
    }

    private static String readSecret(String environmentVariable, String systemProperty, String yamlProperty) {
        String value = System.getenv(environmentVariable);
        if (StringUtils.hasText(value)) {
            return value;
        }
        value = System.getProperty(systemProperty);
        if (StringUtils.hasText(value)) {
            return value;
        }

        Properties localSecret = loadLocalSecretProperties();
        return localSecret.getProperty(yamlProperty);
    }

    private static Properties loadLocalSecretProperties() {
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new FileSystemResource("src/main/resources/application-secret.yml"));

        Properties properties = yamlPropertiesFactoryBean.getObject();
        return properties == null ? new Properties() : properties;
    }
}
