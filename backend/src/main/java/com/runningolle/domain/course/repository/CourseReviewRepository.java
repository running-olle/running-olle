package com.runningolle.domain.course.repository;

import com.runningolle.domain.course.entity.CourseReview;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseReviewRepository extends JpaRepository<CourseReview, UUID> {

    @EntityGraph(attributePaths = {"course", "user", "runningRecord"})
    List<CourseReview> findAllByCourse_IdOrderByCreatedAtDesc(UUID courseId);

    @EntityGraph(attributePaths = {"course", "user", "runningRecord"})
    java.util.Optional<CourseReview> findByIdAndCourse_Id(UUID reviewId, UUID courseId);

    boolean existsByRunningRecord_Id(UUID runningRecordId);

    @Query("select avg(review.rating) from CourseReview review where review.course.id = :courseId")
    Double findAverageRatingByCourseId(@Param("courseId") UUID courseId);

    @Query("""
            select review.course.id as courseId, count(review.id) as reviewCount
            from CourseReview review
            where review.course.id in :courseIds
            group by review.course.id
            """)
    List<CourseReviewCount> countReviewsByCourseIds(@Param("courseIds") Collection<UUID> courseIds);

    interface CourseReviewCount {
        UUID getCourseId();
        long getReviewCount();
    }
}
