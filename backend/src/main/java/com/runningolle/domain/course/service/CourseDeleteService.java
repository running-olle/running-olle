package com.runningolle.domain.course.service;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.repository.CourseRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CourseDeleteService {

    private final CourseRepository courseRepository;

    @Transactional
    public void deleteCourse(UUID userId, UUID courseId) {
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."));
        if (!course.getCreator().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 만든 코스만 삭제할 수 있습니다.");
        }
        course.delete();
    }
}
