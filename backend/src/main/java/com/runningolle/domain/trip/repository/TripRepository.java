package com.runningolle.domain.trip.repository;

import com.runningolle.domain.trip.entity.Trip;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findAllByUserIdOrderByStartDateDesc(UUID userId);
    Optional<Trip> findByIdAndUserId(UUID id, UUID userId);
}
