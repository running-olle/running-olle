package com.runningolle.domain.course.repository;

import com.runningolle.domain.course.entity.CourseReview;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseReviewRepository extends JpaRepository<CourseReview, UUID> {

    boolean existsByRunningRecordId(UUID runningRecordId);

    List<CourseReview> findAllByCourseIdOrderByCreatedAtDesc(UUID courseId);

    Optional<CourseReview> findByIdAndCourseId(UUID reviewId, UUID courseId);

    long countByCourseId(UUID courseId);

    @Query("select avg(cr.rating) from CourseReview cr where cr.course.id = :courseId")
    Double findAverageRatingByCourseId(UUID courseId);
}
