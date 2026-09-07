package com.runningolle.domain.home.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.global.entity.BaseTimeEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
        name = "course_recommendation_documents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_recommendation_documents_course_id_source_type_source_key",
                        columnNames = {"course_id", "source_type", "source_key"}
                )
        },
        indexes = {
                @Index(name = "idx_course_recommendation_documents_course_id", columnList = "course_id"),
                @Index(name = "idx_course_recommendation_documents_embedding_status", columnList = "embedding_status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
public class CourseRecommendationDocument extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private RecommendationDocumentSourceType sourceType;

    @Column(name = "source_key", nullable = false, length = 120)
    private String sourceKey;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "embedding_model", length = 120)
    private String embeddingModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "embedding_status", nullable = false, length = 20)
    @ColumnDefault("'PENDING'")
    private RecommendationDocumentEmbeddingStatus embeddingStatus = RecommendationDocumentEmbeddingStatus.PENDING;

    @Column(name = "embedded_at")
    private LocalDateTime embeddedAt;

    @Column(name = "embedding_failure_reason", length = 500)
    private String embeddingFailureReason;

    @Column(name = "is_deleted", nullable = false)
    @ColumnDefault("false")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static CourseRecommendationDocument create(
            Course course,
            RecommendationDocumentSourceType sourceType,
            String sourceKey,
            String title,
            String content,
            JsonNode metadata
    ) {
        CourseRecommendationDocument document = new CourseRecommendationDocument();
        document.course = course;
        document.sourceType = sourceType;
        document.sourceKey = sourceKey;
        document.title = title;
        document.content = content;
        document.metadata = metadata;
        document.embeddingStatus = RecommendationDocumentEmbeddingStatus.PENDING;
        document.isDeleted = false;
        return document;
    }

    public void markEmbeddingCompleted(String embeddingModel, LocalDateTime embeddedAt) {
        this.embeddingModel = embeddingModel;
        this.embeddingStatus = RecommendationDocumentEmbeddingStatus.COMPLETED;
        this.embeddedAt = embeddedAt;
        this.embeddingFailureReason = null;
    }

    public void markEmbeddingFailed(String failureReason) {
        this.embeddingStatus = RecommendationDocumentEmbeddingStatus.FAILED;
        this.embeddingFailureReason = failureReason;
        this.embeddedAt = null;
    }

    public void updateContent(String title, String content, JsonNode metadata) {
        if (hasSameActiveContent(title, content, metadata)) {
            return;
        }

        this.title = title;
        this.content = content;
        this.metadata = metadata;
        this.embeddingStatus = RecommendationDocumentEmbeddingStatus.PENDING;
        this.embeddedAt = null;
        this.embeddingFailureReason = null;
        this.isDeleted = false;
        this.deletedAt = null;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean hasSameActiveContent(String title, String content, JsonNode metadata) {
        return !Boolean.TRUE.equals(this.isDeleted)
                && Objects.equals(this.title, title)
                && Objects.equals(this.content, content)
                && Objects.equals(this.metadata, metadata);
    }
}
