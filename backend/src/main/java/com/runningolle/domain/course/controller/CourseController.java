package com.runningolle.domain.course.controller;

import com.runningolle.domain.course.dto.CourseBookmarkResponse;
import com.runningolle.domain.course.dto.CourseCreateRequest;
import com.runningolle.domain.course.dto.CourseCreateResponse;
import com.runningolle.domain.course.dto.CourseDetailResponse;
import com.runningolle.domain.course.dto.CourseListFilter;
import com.runningolle.domain.course.dto.CourseListItemResponse;
import com.runningolle.domain.course.dto.CourseListScope;
import com.runningolle.domain.course.service.CourseBookmarkService;
import com.runningolle.domain.course.service.CourseCreateService;
import com.runningolle.domain.course.service.CourseDeleteService;
import com.runningolle.domain.course.service.CourseQueryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseCreateService courseCreateService;
    private final CourseQueryService courseQueryService;
    private final CourseBookmarkService courseBookmarkService;
    private final CourseDeleteService courseDeleteService;

    @GetMapping
    public List<CourseListItemResponse> getCourses(
            Authentication authentication,
            @RequestParam(defaultValue = "ALL") CourseListFilter filter,
            @RequestParam(defaultValue = "AVAILABLE") CourseListScope scope,
            @RequestParam(required = false) String keyword
    ) {
        return courseQueryService.getCourses(UUID.fromString(authentication.getName()), filter, scope, keyword);
    }

    @GetMapping("/{courseId}")
    public CourseDetailResponse getCourse(
            Authentication authentication,
            @PathVariable UUID courseId
    ) {
        return courseQueryService.getCourse(UUID.fromString(authentication.getName()), courseId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseCreateResponse createCourse(
            Authentication authentication,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        return courseCreateService.createCourse(UUID.fromString(authentication.getName()), request);
    }

    @PostMapping("/{courseId}/bookmark")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseBookmarkResponse bookmarkCourse(
            Authentication authentication,
            @PathVariable UUID courseId
    ) {
        return courseBookmarkService.bookmarkCourse(UUID.fromString(authentication.getName()), courseId);
    }

    @DeleteMapping("/{courseId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unbookmarkCourse(
            Authentication authentication,
            @PathVariable UUID courseId
    ) {
        courseBookmarkService.unbookmarkCourse(UUID.fromString(authentication.getName()), courseId);
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(
            Authentication authentication,
            @PathVariable UUID courseId
    ) {
        courseDeleteService.deleteCourse(UUID.fromString(authentication.getName()), courseId);
    }
}
