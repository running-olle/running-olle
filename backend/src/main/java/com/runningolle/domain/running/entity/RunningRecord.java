package com.runningolle.domain.running.entity;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.global.entity.BaseCreatedAtEntity;
import com.runningolle.domain.running.enums.RunningMode;
import com.runningolle.domain.trip.entity.Trip;
import com.runningolle.domain.user.entity.User;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "running_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
public class RunningRecord extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "running_mode", nullable = false, length = 20)
    private RunningMode runningMode;

    @Column(name = "route", nullable = false, columnDefinition = "geometry(LineString,4326)")
    private LineString route;

    @Column(name = "total_distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDistanceKm;

    @Column(name = "total_duration_seconds", nullable = false)
    private Integer totalDurationSeconds;

    @Column(name = "avg_pace", precision = 6, scale = 2)
    private BigDecimal avgPace;

    @Column(name = "calories", precision = 10, scale = 2)
    private BigDecimal calories;

    @Column(name = "elevation_gain_m", precision = 10, scale = 2)
    private BigDecimal elevationGainM;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    public static RunningRecord createFreeRun(
            User user,
            LineString route,
            BigDecimal totalDistanceKm,
            int totalDurationSeconds,
            BigDecimal averagePace,
            BigDecimal calories,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
        return create(
                user,
                null,
                RunningMode.FREE_RUN,
                route,
                totalDistanceKm,
                totalDurationSeconds,
                averagePace,
                calories,
                startedAt,
                endedAt
        );
    }

    public static RunningRecord createCourseRun(
            User user,
            Course course,
            RunningMode runningMode,
            LineString route,
            BigDecimal totalDistanceKm,
            int totalDurationSeconds,
            BigDecimal averagePace,
            BigDecimal calories,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
        if (course == null || runningMode == RunningMode.FREE_RUN) {
            throw new IllegalArgumentException("코스 러닝 기록에는 코스와 코스 러닝 모드가 필요합니다.");
        }
        return create(
                user,
                course,
                runningMode,
                route,
                totalDistanceKm,
                totalDurationSeconds,
                averagePace,
                calories,
                startedAt,
                endedAt
        );
    }

    private static RunningRecord create(
            User user,
            Course course,
            RunningMode runningMode,
            LineString route,
            BigDecimal totalDistanceKm,
            int totalDurationSeconds,
            BigDecimal averagePace,
            BigDecimal calories,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
        RunningRecord record = new RunningRecord();
        record.user = user;
        record.course = course;
        record.runningMode = runningMode;
        record.route = route;
        record.totalDistanceKm = totalDistanceKm;
        record.totalDurationSeconds = totalDurationSeconds;
        record.avgPace = averagePace;
        record.calories = calories;
        record.startedAt = startedAt;
        record.endedAt = endedAt;
        return record;
    }

    public void assignToTrip(Trip trip) {
        this.trip = trip;
    }

    public void attachCreatedCourse(Course course) {
        if (this.course != null || this.runningMode != RunningMode.FREE_RUN) {
            throw new IllegalStateException("자유 러닝 기록에만 새 코스를 연결할 수 있습니다.");
        }
        this.course = course;
        this.runningMode = RunningMode.COURSE_CREATE;
    }
}
