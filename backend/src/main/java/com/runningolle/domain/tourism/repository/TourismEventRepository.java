package com.runningolle.domain.tourism.repository;

import com.runningolle.domain.tourism.entity.TourismEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourismEventRepository extends JpaRepository<TourismEvent, UUID> {

    Optional<TourismEvent> findByContentId(String contentId);

    Optional<TourismEvent> findByIdAndIsDeletedFalse(UUID id);

    long countByIsDeletedFalse();

    List<TourismEvent> findByIsDeletedFalseAndEventEndDateGreaterThanEqual(LocalDate date);
}
