package com.runningolle.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.runningolle.RunningOlleApplication;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseTheme;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseThemeRepository;
import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.repository.CourseRecommendationDocumentRepository;
import com.runningolle.domain.home.service.AiCourseRecommendationReranker;
import com.runningolle.domain.home.service.CourseRecommendationContextRetriever;
import com.runningolle.domain.home.service.CourseRecommendationReranker;
import com.runningolle.domain.home.service.CourseRecommendationSyncService;
import com.runningolle.domain.user.entity.Theme;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.entity.UserTheme;
import com.runningolle.domain.user.entity.UserType;
import com.runningolle.domain.user.entity.UserUserType;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import com.runningolle.domain.user.repository.ThemeRepository;
import com.runningolle.domain.user.repository.UserRepository;
import com.runningolle.domain.user.repository.UserThemeRepository;
import com.runningolle.domain.user.repository.UserTypeRepository;
import com.runningolle.domain.user.repository.UserUserTypeRepository;
import io.micrometer.observation.ObservationRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@EnabledIfSystemProperty(named = "runningolle.external-smoke", matches = "true")
@SpringBootTest(classes = {
        RunningOlleApplication.class,
        HomeRecommendationApiRagSmokeTest.SmokeConfig.class
})
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "home.recommendation.ai-enabled=true",
        "home.recommendation.ai.rerank-enabled=true",
        "home.recommendation.ai.request-timeout-ms=30000",
        "home.recommendation.rag.enabled=true",
        "home.recommendation.rag.candidate-document-limit=5",
        "spring.ai.model.chat=none",
        "spring.ai.model.embedding=none",
        "spring.ai.vectorstore.pgvector.initialize-schema=false"
})
@ImportAutoConfiguration(exclude = PgVectorStoreAutoConfiguration.class)
class HomeRecommendationApiRagSmokeTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final String VECTOR_TABLE = "home_recommendation_api_rag_smoke";
    private static final String SMOKE_PREFIX = "RAG API Smoke ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTypeRepository userTypeRepository;

    @Autowired
    private UserUserTypeRepository userUserTypeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private UserThemeRepository userThemeRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseThemeRepository courseThemeRepository;

    @Autowired
    private CourseRecommendationDocumentRepository courseRecommendationDocumentRepository;

    @Autowired
    private CourseRecommendationSyncService courseRecommendationSyncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cleanSmokeData() {
        cleanup(jdbcTemplate);
    }

    @AfterAll
    static void cleanSmokeDataAfterContext() {
        cleanup(new JdbcTemplate(dataSource()));
    }

    @Test
    void recommendedCoursesApiReturnsRagScoreAndFinalScoreWhenAiAndRagAreEnabled() throws Exception {
        assumeTrue(StringUtils.hasText(readSecret("OPENAI_API_KEY", "openAiApiKey", "spring.ai.openai.api-key")),
                "OpenAI API key is required.");

        User user = persistUserWithPreferences();
        Course coastCourse = persistCourse(
                "Aewol Ocean Photo",
                "A Jeju ocean running route with photo spots, gentle pacing, and travel-friendly scenery.",
                new BigDecimal("6.00"),
                Difficulty.LOW,
                List.of("COAST", "PHOTO")
        );
        Course forestCourse = persistCourse(
                "Saryeoni Forest Shade",
                "A calm forest trail with shade, steady running rhythm, and quiet nature.",
                new BigDecimal("7.00"),
                Difficulty.LOW,
                List.of("FOREST")
        );

        courseRecommendationSyncService.syncPublicCourseRecommendations();

        var authentication = new TestingAuthenticationToken(user.getId().toString(), null);
        authentication.setAuthenticated(true);

        mockMvc.perform(get("/api/home/recommended-courses")
                        .principal(authentication)
                        .param("latitude", "33.5000")
                        .param("longitude", "126.5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0].courseId").exists())
                .andExpect(jsonPath("$.recommendations[0].baseScore").isNumber())
                .andExpect(jsonPath("$.recommendations[0].ragScore").isNumber())
                .andExpect(jsonPath("$.recommendations[0].finalScore").isNumber())
                .andExpect(jsonPath("$.recommendations[0].recommendationReason").isString());

        assertThat(courseRecommendationDocumentRepository
                .findByCourse_IdAndSourceTypeAndSourceKey(
                        coastCourse.getId(),
                        com.runningolle.domain.home.entity.RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                        "course-description"
                )).isPresent();
        assertThat(courseRecommendationDocumentRepository
                .findByCourse_IdAndSourceTypeAndSourceKey(
                        forestCourse.getId(),
                        com.runningolle.domain.home.entity.RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                        "course-description"
                )).isPresent();
    }

    private User persistUserWithPreferences() {
        User user = User.createKakaoUser("rag-api-smoke-" + UUID.randomUUID());
        user.completeOnboarding(
                SMOKE_PREFIX + "User " + UUID.randomUUID(),
                null,
                null,
                PreferredDistance.FROM_5_TO_10KM,
                PreferredDifficulty.EASY,
                true,
                true,
                true,
                false
        );
        user = userRepository.saveAndFlush(user);

        UserType userType = userTypeRepository.findByCode("RELAXED_TRAVELER")
                .orElseGet(() -> userTypeRepository.saveAndFlush(UserType.of("RELAXED_TRAVELER", "Relaxed Traveler")));
        userUserTypeRepository.saveAndFlush(UserUserType.of(user, userType));

        Theme coast = theme("COAST");
        Theme photo = theme("PHOTO");
        userThemeRepository.saveAndFlush(UserTheme.of(user, coast));
        userThemeRepository.saveAndFlush(UserTheme.of(user, photo));
        return user;
    }

    private Course persistCourse(
            String name,
            String description,
            BigDecimal distanceKm,
            Difficulty difficulty,
            List<String> themeCodes
    ) {
        User creator = userRepository.saveAndFlush(User.createKakaoUser("rag-api-smoke-creator-" + UUID.randomUUID()));
        Course course = courseRepository.saveAndFlush(Course.create(
                creator,
                SMOKE_PREFIX + name + " " + UUID.randomUUID(),
                description,
                CourseType.RUNNING_COURSE,
                distanceKm,
                45,
                new BigDecimal("20.00"),
                difficulty,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                route(),
                startPoint(),
                null,
                true
        ));
        for (String themeCode : themeCodes) {
            courseThemeRepository.saveAndFlush(CourseTheme.of(course, theme(themeCode)));
        }
        return course;
    }

    private Theme theme(String code) {
        return themeRepository.findByCode(code)
                .orElseGet(() -> themeRepository.saveAndFlush(Theme.create(code, code)));
    }

    private org.locationtech.jts.geom.LineString route() {
        var route = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(126.50, 33.50),
                new Coordinate(126.51, 33.51)
        });
        route.setSRID(4326);
        return route;
    }

    private org.locationtech.jts.geom.Point startPoint() {
        var startPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(126.50, 33.50));
        startPoint.setSRID(4326);
        return startPoint;
    }

    private static void cleanup(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                delete from course_recommendation_documents
                where course_id in (select id from courses where name like ?)
                """, SMOKE_PREFIX + "%");
        jdbcTemplate.update("""
                delete from course_themes
                where course_id in (select id from courses where name like ?)
                """, SMOKE_PREFIX + "%");
        jdbcTemplate.update("delete from courses where name like ?", SMOKE_PREFIX + "%");
        jdbcTemplate.update("delete from user_themes where user_id in (select id from users where nickname like ?)", SMOKE_PREFIX + "%");
        jdbcTemplate.update("delete from user_user_types where user_id in (select id from users where nickname like ?)", SMOKE_PREFIX + "%");
        jdbcTemplate.update("delete from users where nickname like ?", SMOKE_PREFIX + "%");
        jdbcTemplate.update("delete from users where kakao_id like ?", "rag-api-smoke-creator-%");
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + VECTOR_TABLE);
    }

    private static DriverManagerDataSource dataSource() {
        Properties properties = loadLocalSecretProperties();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(required(properties, "spring.datasource.url"));
        dataSource.setUsername(required(properties, "spring.datasource.username"));
        dataSource.setPassword(required(properties, "spring.datasource.password"));
        return dataSource;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
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

    @TestConfiguration
    static class SmokeConfig {

        @Bean
        @Primary
        VectorStore smokeVectorStore(JdbcTemplate jdbcTemplate) {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS hstore");
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + VECTOR_TABLE);
            jdbcTemplate.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS public.%s (
                        id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
                        content text,
                        metadata json,
                        embedding vector(3)
                    )
                    """, VECTOR_TABLE));
            jdbcTemplate.execute(String.format("""
                    CREATE INDEX IF NOT EXISTS %s_hnsw_idx
                        ON public.%s USING HNSW (embedding vector_cosine_ops)
                    """, VECTOR_TABLE, VECTOR_TABLE));

            PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, new FixedEmbeddingModel())
                    .vectorTableName(VECTOR_TABLE)
                    .dimensions(3)
                    .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                    .indexType(PgVectorStore.PgIndexType.HNSW)
                    .initializeSchema(false)
                    .build();
            vectorStore.afterPropertiesSet();
            return vectorStore;
        }

        @Bean
        @Primary
        ChatClient.Builder smokeChatClientBuilder() {
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(OpenAiApi.builder()
                            .apiKey(required(loadLocalSecretProperties(), "spring.ai.openai.api-key"))
                            .build())
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model("gpt-4o-mini")
                            .temperature(0.0)
                            .maxTokens(700)
                            .build())
                    .toolCallingManager(ToolCallingManager.builder().build())
                    .retryTemplate(RetryTemplate.defaultInstance())
                    .observationRegistry(ObservationRegistry.NOOP)
                    .build();
            return ChatClient.builder(chatModel);
        }

        @Bean
        @Primary
        CourseRecommendationReranker smokeCourseRecommendationReranker(
                ChatClient.Builder chatClientBuilder,
                CourseRecommendationContextRetriever courseRecommendationContextRetriever,
                HomeRecommendationProperties homeRecommendationProperties
        ) {
            return new AiCourseRecommendationReranker(
                    chatClientBuilder,
                    courseRecommendationContextRetriever,
                    homeRecommendationProperties
            );
        }
    }

    private static class FixedEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new java.util.ArrayList<>();
            for (int i = 0; i < request.getInstructions().size(); i++) {
                embeddings.add(new Embedding(embedText(request.getInstructions().get(i)), i));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return embedText(document.getText());
        }

        @Override
        public int dimensions() {
            return 3;
        }

        private static float[] embedText(String text) {
            String normalized = text == null ? "" : text.toLowerCase();
            return new float[]{
                    normalized.contains("coast") || normalized.contains("ocean") || normalized.contains("photo") ? 1.0f : 0.0f,
                    normalized.contains("forest") || normalized.contains("shade") || normalized.contains("trail") ? 1.0f : 0.0f,
                    normalized.contains("route") || normalized.contains("running") ? 1.0f : 0.0f
            };
        }
    }
}
