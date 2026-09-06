package com.runningolle.domain.home.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "home.recommendation")
public class HomeRecommendationProperties {

    private boolean aiEnabled = false;

    private boolean manualSyncEnabled = false;

    @Min(1)
    private int rerankCandidateLimit = 5;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double baseScoreWeight = 0.75;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double ragScoreWeight = 0.25;

    @Valid
    private final Ai ai = new Ai();

    @Valid
    private final Rag rag = new Rag();

    public boolean isAiRerankingEnabled() {
        return aiEnabled && ai.isRerankEnabled();
    }

    @Getter
    @Setter
    public static class Ai {

        private boolean rerankEnabled = true;

        private String provider = "openai";

        @Min(100)
        private int requestTimeoutMs = 5000;
    }

    @Getter
    @Setter
    public static class Rag {

        private boolean enabled = false;

        private String vectorStore = "pgvector";

        private boolean metadataFilterRequired = true;

        @Min(1)
        private int candidateDocumentLimit = 5;

        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private double similarityThreshold = 0.0;
    }
}
