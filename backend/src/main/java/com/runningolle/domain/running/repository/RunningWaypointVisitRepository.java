package com.runningolle.domain.running.repository;

import com.runningolle.domain.running.entity.RunningWaypointVisit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunningWaypointVisitRepository extends JpaRepository<RunningWaypointVisit, UUID> {
    long countByRunningRecordTripId(UUID tripId);

    @EntityGraph(attributePaths = {"runningRecord.course", "courseWaypoint.course"})
    List<RunningWaypointVisit> findAllByRunningRecordUserIdOrderByVisitedAtDesc(UUID userId);
}
