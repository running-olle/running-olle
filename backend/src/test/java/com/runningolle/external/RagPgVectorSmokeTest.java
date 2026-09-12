package com.runningolle.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StringUtils;

@EnabledIfSystemProperty(named = "runningolle.external-smoke", matches = "true")
class RagPgVectorSmokeTest {

    @Test
    void pgVectorStoreCanSearchOnlyWithinCandidateCourseIds() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS hstore");
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");

        String tableName = "recommendation_vector_smoke_" + UUID.randomUUID().toString().replace("-", "");
        try {
            jdbcTemplate.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS public.%s (
                        id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
                        content text,
                        metadata json,
                        embedding vector(3)
                    )
                    """, tableName));
            jdbcTemplate.execute(String.format("""
                    CREATE INDEX IF NOT EXISTS %s_hnsw_idx
                        ON public.%s USING HNSW (embedding vector_cosine_ops)
                    """, tableName, tableName));

            PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, new FixedEmbeddingModel())
                    .vectorTableName(tableName)
                    .dimensions(3)
                    .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                    .indexType(PgVectorStore.PgIndexType.HNSW)
                    .initializeSchema(false)
                    .build();
            vectorStore.afterPropertiesSet();

            String coastCourseId = UUID.randomUUID().toString();
            String forestCourseId = UUID.randomUUID().toString();
            vectorStore.add(List.of(
                    Document.builder()
                            .text("coast ocean photo route")
                            .metadata("courseId", coastCourseId)
                            .metadata("sourceType", "COURSE_DESCRIPTION")
                            .build(),
                    Document.builder()
                            .text("forest shade trail route")
                            .metadata("courseId", forestCourseId)
                            .metadata("sourceType", "COURSE_DESCRIPTION")
                            .build()
            ));

            FilterExpressionBuilder filter = new FilterExpressionBuilder();
            List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
                    .query("forest shade")
                    .topK(2)
                    .similarityThresholdAll()
                    .filterExpression(filter.eq("courseId", forestCourseId).build())
                    .build());

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getMetadata().get("courseId")).isEqualTo(forestCourseId);
        } finally {
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
        }
    }

    private DriverManagerDataSource dataSource() {
        Properties properties = loadLocalSecretProperties();
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(required(properties, "spring.datasource.url"));
        dataSource.setUsername(required(properties, "spring.datasource.username"));
        dataSource.setPassword(required(properties, "spring.datasource.password"));
        return dataSource;
    }

    private String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }

    private Properties loadLocalSecretProperties() {
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new FileSystemResource("src/main/resources/application-secret.yml"));

        Properties properties = yamlPropertiesFactoryBean.getObject();
        return properties == null ? new Properties() : properties;
    }

    private static class FixedEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = request.getInstructions().stream()
                    .map(text -> new Embedding(embedText(text), request.getInstructions().indexOf(text)))
                    .toList();
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
