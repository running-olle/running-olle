package com.runningolle.domain.course.repository;

import com.runningolle.domain.course.entity.Course;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {
}
