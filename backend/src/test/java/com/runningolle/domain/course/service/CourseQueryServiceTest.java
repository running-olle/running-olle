package com.runningolle.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.runningolle.domain.course.dto.CourseListFilter;
import com.runningolle.domain.course.dto.CourseListItemResponse;
import com.runningolle.domain.course.dto.CourseListScope;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseBookmark;
import com.runningolle.domain.course.entity.CourseWaypoint;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.course.repository.CourseBookmarkRepository;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseWaypointRepository;
import com.runningolle.domain.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseQueryServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseWaypointRepository courseWaypointRepository;

    @Mock
    private CourseBookmarkRepository courseBookmarkRepository;

    private CourseQueryService courseQueryService;

    @BeforeEach
    void setUp() {
        courseQueryService = new CourseQueryService(
                courseRepository,
                courseWaypointRepository,
                courseBookmarkRepository
        );
    }

    @Test
    void returnsLibraryCoursesWithOwnershipBookmarkAndWaypointNames() {
        UUID myCourseId = UUID.randomUUID();
        UUID savedCourseId = UUID.randomUUID();
        Course myCourse = course(myCourseId, USER_ID, "내가 만든 성산 코스", CourseType.RUNNING_COURSE);
        Course savedCourse = course(savedCourseId, UUID.randomUUID(), "저장한 협재 코스", CourseType.SPOT_COURSE);
        List<UUID> courseIds = List.of(myCourseId, savedCourseId);

        given(courseRepository.findVisibleCourses(USER_ID, null, false, true, null))
                .willReturn(List.of(myCourse, savedCourse));
        given(courseWaypointRepository.findByCourse_IdInOrderByCourse_IdAscOrderIndexAsc(eq(courseIds)))
                .willReturn(List.of(
                        waypoint(myCourse, "성산일출봉"),
                        waypoint(myCourse, "광치기해변"),
                        waypoint(savedCourse, "협재해수욕장")
                ));
        given(courseBookmarkRepository.findAllByUser_IdAndCourse_IdIn(USER_ID, courseIds))
                .willReturn(List.of(bookmark(savedCourse)));

        List<CourseListItemResponse> responses = courseQueryService.getCourses(
                USER_ID,
                CourseListFilter.ALL,
                CourseListScope.LIBRARY,
                null
        );

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).createdByMe()).isTrue();
        assertThat(responses.get(0).bookmarkedByMe()).isFalse();
        assertThat(responses.get(0).waypointNames()).containsExactly("성산일출봉", "광치기해변");
        assertThat(responses.get(1).createdByMe()).isFalse();
        assertThat(responses.get(1).bookmarkedByMe()).isTrue();
        assertThat(responses.get(1).bookmarkId()).isNotNull();
        assertThat(responses.get(1).waypointNames()).containsExactly("협재해수욕장");
    }

    @Test
    void createdFilterRequestsCreatedOnlyCourses() {
        given(courseRepository.findVisibleCourses(USER_ID, null, true, false, null))
                .willReturn(List.of());

        courseQueryService.getCourses(USER_ID, CourseListFilter.CREATED, CourseListScope.AVAILABLE, null);

        verify(courseRepository).findVisibleCourses(USER_ID, null, true, false, null);
    }

    @Test
    void keywordIsTrimmedAndLowercasedBeforeQueryingCourses() {
        given(courseRepository.findVisibleCourses(USER_ID, null, false, false, "seongsan"))
                .willReturn(List.of());

        courseQueryService.getCourses(USER_ID, CourseListFilter.ALL, CourseListScope.AVAILABLE, "  SeongSan  ");

        verify(courseRepository).findVisibleCourses(USER_ID, null, false, false, "seongsan");
    }

    private static Course course(UUID id, UUID creatorId, String name, CourseType courseType) {
        User creator = User.createKakaoUser("kakao-" + creatorId);
        ReflectionTestUtils.setField(creator, "id", creatorId);

        var route = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(126.5, 33.5),
                new Coordinate(126.51, 33.51)
        });
        route.setSRID(4326);
        var startPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(126.5, 33.5));
        startPoint.setSRID(4326);

        Course course = Course.create(
                creator,
                name,
                "제주를 달리는 코스",
                courseType,
                new BigDecimal("5.20"),
                52,
                new BigDecimal("38.00"),
                Difficulty.LOW,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                route,
                startPoint,
                null,
                true
        );
        ReflectionTestUtils.setField(course, "id", id);
        ReflectionTestUtils.setField(course, "createdAt", LocalDateTime.of(2026, 8, 23, 10, 0));
        return course;
    }

    private static CourseWaypoint waypoint(Course course, String name) {
        var point = GEOMETRY_FACTORY.createPoint(new Coordinate(126.5, 33.5));
        point.setSRID(4326);
        return CourseWaypoint.create(
                course,
                name,
                null,
                point,
                0,
                BigDecimal.ZERO,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static CourseBookmark bookmark(Course course) {
        try {
            var constructor = CourseBookmark.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            CourseBookmark bookmark = constructor.newInstance();
            ReflectionTestUtils.setField(bookmark, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(bookmark, "course", course);
            return bookmark;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("CourseBookmark 테스트 객체를 만들 수 없습니다.", exception);
        }
    }
}
