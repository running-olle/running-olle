package com.runningolle.domain.course.service;

import com.runningolle.domain.course.dto.CourseListFilter;
import com.runningolle.domain.course.dto.CourseListItemResponse;
import com.runningolle.domain.course.dto.CourseListScope;
import com.runningolle.domain.course.dto.CourseDetailResponse;
import com.runningolle.domain.course.dto.CourseWaypointResponse;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseBookmark;
import com.runningolle.domain.course.entity.CourseWaypoint;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.repository.CourseBookmarkRepository;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseWaypointRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CourseQueryService {

    private final CourseRepository courseRepository;
    private final CourseWaypointRepository courseWaypointRepository;
    private final CourseBookmarkRepository courseBookmarkRepository;

    @Transactional(readOnly = true)
    public List<CourseListItemResponse> getCourses(UUID userId, CourseListFilter filter, CourseListScope scope, String keyword) {
        CourseType courseType = toCourseType(filter);
        boolean createdOnly = filter == CourseListFilter.CREATED;
        boolean libraryOnly = scope == CourseListScope.LIBRARY;
        String normalizedKeyword = normalizeKeyword(keyword);

        List<Course> courses = courseRepository.findVisibleCourses(
                userId,
                courseType,
                createdOnly,
                libraryOnly,
                normalizedKeyword
        );
        if (courses.isEmpty()) {
            return List.of();
        }

        List<UUID> courseIds = courses.stream().map(Course::getId).toList();
        Map<UUID, List<CourseWaypointResponse>> waypointsByCourseId = waypointsByCourseId(courseIds);
        Map<UUID, UUID> bookmarkIdByCourseId = bookmarkIdByCourseId(userId, courseIds);
        Set<UUID> bookmarkedCourseIds = bookmarkIdByCourseId.keySet();

        return courses.stream()
                .map(course -> CourseListItemResponse.from(
                        course,
                        course.getCreator().getId().equals(userId),
                        bookmarkedCourseIds.contains(course.getId()),
                        bookmarkIdByCourseId.get(course.getId()),
                        waypointsByCourseId.getOrDefault(course.getId(), List.of())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourse(UUID userId, UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."));
        if (Boolean.TRUE.equals(course.getIsDeleted())
                || (!Boolean.TRUE.equals(course.getIsPublic()) && !course.getCreator().getId().equals(userId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다.");
        }

        List<CourseWaypointResponse> waypoints = courseWaypointRepository.findByCourse_IdOrderByOrderIndexAsc(courseId)
                .stream()
                .map(CourseWaypointResponse::from)
                .toList();
        UUID bookmarkId = bookmarkIdByCourseId(userId, List.of(courseId)).get(courseId);

        return CourseDetailResponse.from(
                course,
                course.getCreator().getId().equals(userId),
                bookmarkId != null,
                bookmarkId,
                waypoints
        );
    }

    private CourseType toCourseType(CourseListFilter filter) {
        return switch (filter) {
            case RUNNING_COURSE -> CourseType.RUNNING_COURSE;
            case SPOT_COURSE -> CourseType.SPOT_COURSE;
            case ALL, CREATED -> null;
        };
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase();
    }

    private Map<UUID, List<CourseWaypointResponse>> waypointsByCourseId(List<UUID> courseIds) {
        List<CourseWaypoint> waypoints = courseWaypointRepository.findByCourse_IdInOrderByCourse_IdAscOrderIndexAsc(courseIds);
        Map<UUID, List<CourseWaypointResponse>> waypointsByCourseId = new LinkedHashMap<>();
        for (CourseWaypoint waypoint : waypoints) {
            waypointsByCourseId.computeIfAbsent(waypoint.getCourse().getId(), ignored -> new ArrayList<>())
                    .add(CourseWaypointResponse.from(waypoint));
        }
        return waypointsByCourseId;
    }

    private Map<UUID, UUID> bookmarkIdByCourseId(UUID userId, List<UUID> courseIds) {
        List<CourseBookmark> bookmarks = courseBookmarkRepository.findAllByUser_IdAndCourse_IdIn(userId, courseIds);
        Map<UUID, UUID> bookmarkIdByCourseId = new LinkedHashMap<>();
        for (CourseBookmark bookmark : bookmarks) {
            bookmarkIdByCourseId.put(bookmark.getCourse().getId(), bookmark.getId());
        }
        return bookmarkIdByCourseId;
    }
}
