package com.runningolle.domain.course.entity;

import com.runningolle.global.entity.BaseTimeEntity;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.user.entity.User;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
        name = "course_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_reviews_running_record_id",
                columnNames = "running_record_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
public class CourseReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "running_record_id", nullable = false)
    private RunningRecord runningRecord;

    @DecimalMin("0.5")
    @DecimalMax("5.0")
    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    public static CourseReview create(User user, Course course, RunningRecord runningRecord, BigDecimal rating, String content) {
        CourseReview courseReview = new CourseReview();
        courseReview.user = user;
        courseReview.course = course;
        courseReview.runningRecord = runningRecord;
        courseReview.rating = rating;
        courseReview.content = content;
        return courseReview;
    }

    public void update(BigDecimal rating, String content) {
        this.rating = rating;
        this.content = content;
    }
}
