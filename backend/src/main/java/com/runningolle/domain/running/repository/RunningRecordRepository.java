package com.runningolle.domain.running.repository;

import com.runningolle.domain.running.entity.RunningRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RunningRecordRepository extends JpaRepository<RunningRecord, UUID> {

    @EntityGraph(attributePaths = {"course", "trip"})
    Optional<RunningRecord> findByIdAndUserId(UUID id, UUID userId);
    List<RunningRecord> findTop10ByUserIdOrderByStartedAtDesc(UUID userId);
    List<RunningRecord> findByUserId(UUID userId);
    long countByUserId(UUID userId);

    @EntityGraph(attributePaths = "course")
    List<RunningRecord> findAllByUserIdOrderByStartedAtDesc(UUID userId);

    @EntityGraph(attributePaths = "course")
    List<RunningRecord> findAllByTripIdOrderByStartedAtDesc(UUID tripId);

    long countByTripId(UUID tripId);

    List<RunningRecord> findAllByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
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
}
