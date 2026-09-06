package com.runningolle.domain.running.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseWaypoint;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseWaypointRepository;
import com.runningolle.domain.running.dto.SaveRunningCourseRequest;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.enums.RunningMode;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import com.runningolle.domain.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RunningCourseSaveServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    private RunningRecordRepository runningRecordRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseWaypointRepository courseWaypointRepository;

    private RunningCourseSaveService service;

    @BeforeEach
    void setUp() {
        service = new RunningCourseSaveService(runningRecordRepository, courseRepository, courseWaypointRepository);
        lenient().when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            ReflectionTestUtils.setField(course, "id", COURSE_ID);
            return course;
        });
    }

    @Test
    void savesExactFreeRunningRouteAsNamedCourseAndConnectsRecord() {
        RunningRecord record = freeRunRecord();
        given(runningRecordRepository.findByIdAndUserId(RECORD_ID, USER_ID)).willReturn(Optional.of(record));

        var response = service.saveAsCourse(
                USER_ID,
                RECORD_ID,
                new SaveRunningCourseRequest("  바다 노을 코스  ", "  다시 달리고 싶은 길  ", false)
        );

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(courseCaptor.capture());
        Course course = courseCaptor.getValue();
        assertThat(response.courseId()).isEqualTo(COURSE_ID);
        assertThat(course.getName()).isEqualTo("바다 노을 코스");
        assertThat(course.getDescription()).isEqualTo("다시 달리고 싶은 길");
        assertThat(course.getIsPublic()).isFalse();
        assertThat(course.getRoute().equalsExact(record.getRoute())).isTrue();
        assertThat(course.getCompletionCount()).isEqualTo(1);
        assertThat(record.getCourse()).isSameAs(course);
        assertThat(record.getRunningMode()).isEqualTo(RunningMode.COURSE_CREATE);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CourseWaypoint>> waypointCaptor = ArgumentCaptor.forClass(List.class);
        verify(courseWaypointRepository).saveAll(waypointCaptor.capture());
        assertThat(waypointCaptor.getValue()).extracting(CourseWaypoint::getName)
                .containsExactly("출발점", "도착점");
    }

    @Test
    void rejectsRecordThatAlreadyHasCourse() {
        RunningRecord record = freeRunRecord();
        record.attachCreatedCourse(course());
        given(runningRecordRepository.findByIdAndUserId(RECORD_ID, USER_ID)).willReturn(Optional.of(record));

        assertThatThrownBy(() -> service.saveAsCourse(
                USER_ID,
                RECORD_ID,
                new SaveRunningCourseRequest("중복 코스", null, true)
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 코스가 연결된 러닝 기록입니다.");

        verify(courseRepository, never()).save(any());
    }

    private static RunningRecord freeRunRecord() {
        User user = User.createKakaoUser("kakao-" + USER_ID);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        var route = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(126.5312, 33.4996),
                new Coordinate(126.5400, 33.5100),
                new Coordinate(126.5500, 33.5150)
        });
        route.setSRID(4326);
        RunningRecord record = RunningRecord.createFreeRun(
                user,
                route,
                new BigDecimal("3.20"),
                1_920,
                new BigDecimal("6.00"),
                new BigDecimal("180.00"),
                LocalDateTime.parse("2026-08-27T09:00:00"),
                LocalDateTime.parse("2026-08-27T09:32:00")
        );
        ReflectionTestUtils.setField(record, "id", RECORD_ID);
        return record;
    }

    private static Course course() {
        RunningRecord record = freeRunRecord();
        var startPoint = GEOMETRY_FACTORY.createPoint(record.getRoute().getCoordinateN(0));
        return Course.create(
                record.getUser(), "기존 코스", null,
                com.runningolle.domain.course.enums.CourseType.RUNNING_COURSE,
                record.getTotalDistanceKm(), 32, BigDecimal.ZERO,
                com.runningolle.domain.course.enums.Difficulty.LOW,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                record.getRoute(), startPoint, null, true
        );
    }
}
