package com.runningolle.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.runningolle.RunningOlleApplication;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseTheme;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseThemeRepository;
import com.runningolle.domain.home.entity.CourseRecommendationDocument;
import com.runningolle.domain.home.entity.RecommendationDocumentEmbeddingStatus;
import com.runningolle.domain.home.entity.RecommendationDocumentSourceType;
import com.runningolle.domain.home.repository.CourseRecommendationDocumentRepository;
import com.runningolle.domain.home.service.CourseRecommendationSyncService;
import com.runningolle.domain.user.entity.Theme;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.repository.ThemeRepository;
import com.runningolle.domain.user.repository.UserRepository;
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
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@EnabledIfSystemProperty(named = "runningolle.external-smoke", matches = "true")
@SpringBootTest(classes = {
        RunningOlleApplication.class,
        RagRecommendationSyncSmokeTest.PgVectorSmokeConfig.class
})
@TestPropertySource(properties = {
        "home.recommendation.rag.enabled=true",
        "spring.ai.model.chat=none",
        "spring.ai.model.embedding=none",
        "spring.ai.vectorstore.pgvector.initialize-schema=false"
})
@ImportAutoConfiguration(exclude = PgVectorStoreAutoConfiguration.class)
class RagRecommendationSyncSmokeTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final String VECTOR_TABLE = "recommendation_sync_vector_smoke";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private CourseThemeRepository courseThemeRepository;

    @Autowired
    private CourseRecommendationDocumentRepository courseRecommendationDocumentRepository;

    @Autowired
    private CourseRecommendationSyncService courseRecommendationSyncService;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void dropSmokeVectorTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + VECTOR_TABLE);
    }

    @AfterAll
    static void dropSmokeVectorTableAfterContext() {
        new JdbcTemplate(dataSource()).execute("DROP TABLE IF EXISTS " + VECTOR_TABLE);
    }

    @Test
    @Transactional
    void syncCreatesDescriptionDocumentAndVectorStoreEntryForPublicCourse() {
        Course course = persistPublicCourseWithDescription();

        courseRecommendationSyncService.syncPublicCourseRecommendations();

        CourseRecommendationDocument document = courseRecommendationDocumentRepository
                .findByCourse_IdAndSourceTypeAndSourceKey(
                        course.getId(),
                        RecommendationDocumentSourceType.COURSE_DESCRIPTION,
                        "course-description"
                )
                .orElseThrow();
        assertThat(document.getContent()).contains("ocean", "photo");
        assertThat(document.getEmbeddingStatus()).isEqualTo(RecommendationDocumentEmbeddingStatus.COMPLETED);

        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
                .query("ocean photo")
                .topK(5)
                .similarityThresholdAll()
                .filterExpression(filter.eq("courseId", course.getId().toString()).build())
                .build());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMetadata().get("courseId")).isEqualTo(course.getId().toString());
        assertThat(results.get(0).getText()).contains("ocean", "photo");
    }

    private Course persistPublicCourseWithDescription() {
        User creator = userRepository.saveAndFlush(User.createKakaoUser("rag-sync-smoke-" + UUID.randomUUID()));
        Theme coast = themeRepository.findByCode("COAST")
                .orElseGet(() -> themeRepository.saveAndFlush(Theme.create("COAST", "Coast")));

        Course course = courseRepository.saveAndFlush(Course.create(
                creator,
                "RAG Sync Smoke Course " + UUID.randomUUID(),
                "A Jeju ocean route with photo spots for recommendation sync smoke.",
                CourseType.RUNNING_COURSE,
                new BigDecimal("6.00"),
                45,
                new BigDecimal("30.00"),
                Difficulty.LOW,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                route(),
                startPoint(),
                null,
                true
        ));
        courseThemeRepository.saveAndFlush(CourseTheme.of(course, coast));
        return course;
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

    private static Properties loadLocalSecretProperties() {
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new FileSystemResource("src/main/resources/application-secret.yml"));

        Properties properties = yamlPropertiesFactoryBean.getObject();
        return properties == null ? new Properties() : properties;
    }

    @TestConfiguration
    static class PgVectorSmokeConfig {

        @Bean
        @Primary
        VectorStore smokeVectorStore(JdbcTemplate jdbcTemplate) {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS hstore");
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + VECTOR_TABLE);

            PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, new FixedEmbeddingModel())
                    .vectorTableName(VECTOR_TABLE)
                    .dimensions(3)
                    .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                    .indexType(PgVectorStore.PgIndexType.NONE)
                    .initializeSchema(true)
                    .build();
            vectorStore.afterPropertiesSet();
            return vectorStore;
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
