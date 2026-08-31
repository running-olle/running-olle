package com.runningolle.domain.course.service;

import com.runningolle.domain.course.dto.CourseBookmarkResponse;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseBookmark;
import com.runningolle.domain.course.repository.CourseBookmarkRepository;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CourseBookmarkService {

    private final CourseBookmarkRepository courseBookmarkRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourseBookmarkResponse bookmarkCourse(UUID userId, UUID courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."));

        if (!Boolean.TRUE.equals(course.getIsPublic()) && !course.getCreator().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다.");
        }
        if (course.getCreator().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내가 만든 코스는 별도로 저장할 필요가 없습니다.");
        }

        return courseBookmarkRepository.findByUser_IdAndCourse_Id(userId, courseId)
                .map(bookmark -> new CourseBookmarkResponse(bookmark.getId()))
                .orElseGet(() -> {
                    CourseBookmark bookmark = courseBookmarkRepository.save(CourseBookmark.create(user, course));
                    return new CourseBookmarkResponse(bookmark.getId());
                });
    }
}
