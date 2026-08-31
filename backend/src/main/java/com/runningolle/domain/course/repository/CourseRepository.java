package com.runningolle.domain.course.repository;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.enums.CourseType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByIdAndIsDeletedFalse(UUID id);
    List<Course> findTop10ByIsDeletedFalseAndIsPublicTrueOrderByCreatedAtDesc();

    @Query("""
            select distinct c
            from Course c
            where c.isDeleted = false
              and (:createdOnly = false or c.creator.id = :userId)
              and (:courseType is null or c.courseType = :courseType)
              and (
                    (:libraryOnly = false and c.isPublic = true)
                    or (:libraryOnly = true and (
                        c.creator.id = :userId
                        or exists (
                            select 1
                            from CourseBookmark bookmark
                            where bookmark.course = c
                              and bookmark.user.id = :userId
                        )
                    ))
                  )
            order by c.createdAt desc
            """)
    List<Course> findVisibleCourses(
            @Param("userId") UUID userId,
            @Param("courseType") CourseType courseType,
            @Param("createdOnly") boolean createdOnly,
            @Param("libraryOnly") boolean libraryOnly
    );
}
