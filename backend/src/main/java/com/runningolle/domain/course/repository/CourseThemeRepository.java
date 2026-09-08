package com.runningolle.domain.course.repository;

import com.runningolle.domain.course.entity.CourseTheme;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseThemeRepository extends JpaRepository<CourseTheme, UUID> {

    @EntityGraph(attributePaths = "theme")
    List<CourseTheme> findAllByCourse_IdIn(Collection<UUID> courseIds);
}
