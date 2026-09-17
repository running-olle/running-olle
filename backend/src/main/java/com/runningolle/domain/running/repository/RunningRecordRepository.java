package com.runningolle.domain.running.repository;

import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.running.entity.RunningRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RunningRecordRepository extends JpaRepository<RunningRecord, UUID> {

    @EntityGraph(attributePaths = {"course", "trip"})
    Optional<RunningRecord> findByIdAndUserId(UUID id, UUID userId);
    List<RunningRecord> findTop10ByUserIdOrderByStartedAtDesc(UUID userId);
    List<RunningRecord> findByUserId(UUID userId);
    long countByUserId(UUID userId);
    long countByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
            UUID userId, LocalDateTime start, LocalDateTime endExclusive
    );

    @EntityGraph(attributePaths = "course")
    List<RunningRecord> findAllByUserIdOrderByStartedAtDesc(UUID userId);

    @EntityGraph(attributePaths = "course")
    List<RunningRecord> findAllByTripIdOrderByStartedAtDesc(UUID tripId);

    long countByTripId(UUID tripId);

    @Query("""
            select r.course.id as courseId,
                   r.course.name as courseName,
                   r.course.distanceKm as distanceKm,
                   r.course.difficulty as difficulty,
                   r.course.thumbnailImageUrl as thumbnailImageUrl,
                   count(distinct r.user.id) as participantCount
            from RunningRecord r
            where r.course is not null
              and r.course.isPublic = true
              and r.course.isDeleted = false
              and r.startedAt >= :startedAt
              and r.startedAt < :endedAt
            group by r.course.id,
                     r.course.name,
                     r.course.distanceKm,
                     r.course.difficulty,
                     r.course.thumbnailImageUrl
            order by count(distinct r.user.id) desc,
                     r.course.name asc,
                     r.course.id asc
            """)
    List<PopularCourseProjection> findPopularCourses(
            @Param("startedAt") LocalDateTime startedAt,
            @Param("endedAt") LocalDateTime endedAt,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "course")
    List<RunningRecord> findAllByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
            UUID userId, LocalDateTime start, LocalDateTime endExclusive
    );

    @Query("""
            select r.user.id as userId,
                   coalesce(sum(r.totalDistanceKm), 0) as totalDistanceKm,
                   avg(r.avgPace) as averagePaceMinutes
            from RunningRecord r
            where r.user.id in :userIds
            group by r.user.id
            """)
    List<UserRunningStatsProjection> aggregateStatsByUserIds(List<UUID> userIds);

    interface UserRunningStatsProjection {
        UUID getUserId();
        BigDecimal getTotalDistanceKm();
        BigDecimal getAveragePaceMinutes();
    }

    interface PopularCourseProjection {
        UUID getCourseId();
        String getCourseName();
        BigDecimal getDistanceKm();
        Difficulty getDifficulty();
        String getThumbnailImageUrl();
        long getParticipantCount();
    }
}
