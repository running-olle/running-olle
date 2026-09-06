package com.runningolle.domain.course.repository;

import com.runningolle.domain.course.entity.CourseReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReviewRepository extends JpaRepository<CourseReview, UUID> {

    @EntityGraph(attributePaths = {"course", "user"})
    List<CourseReview> findAllByCourse_IdOrderByCreatedAtDesc(UUID courseId);
}
