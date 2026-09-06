package com.runningolle.domain.running.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.running.dto.CreateRunningRecordRequest;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.enums.RunningMode;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
class RunningRecordServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final OffsetDateTime STARTED_AT = OffsetDateTime.parse("2026-08-27T00:00:00Z");
    private static final OffsetDateTime ENDED_AT = OffsetDateTime.parse("2026-08-27T00:32:00Z");

    @Mock
    private RunningRecordRepository runningRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    private RunningRecordService runningRecordService;

    @BeforeEach
    void setUp() {
        runningRecordService = new RunningRecordService(
                runningRecordRepository,
                userRepository,
                courseRepository
        );
        lenient().when(runningRecordRepository.save(any(RunningRecord.class))).thenAnswer(invocation -> {
            RunningRecord record = invocation.getArgument(0);
            ReflectionTestUtils.setField(record, "id", UUID.randomUUID());
            return record;
        });
    }

    @Test
    void createsFreeRunRecordWhenCourseIdIsAbsent() {
        User user = user(USER_ID);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        runningRecordService.createRecord(USER_ID, request(null, null));

        ArgumentCaptor<RunningRecord> recordCaptor = ArgumentCaptor.forClass(RunningRecord.class);
        verify(runningRecordRepository).save(recordCaptor.capture());
        RunningRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getUser()).isEqualTo(user);
        assertThat(savedRecord.getCourse()).isNull();
        assertThat(savedRecord.getRunningMode()).isEqualTo(RunningMode.FREE_RUN);
        assertThat(savedRecord.getTotalDistanceKm()).isEqualByComparingTo(new BigDecimal("3.20"));
    }

    @Test
    void createsCourseSelectRecordAndIncrementsCompletionCountWhenCourseIdIsProvided() {
        User user = user(USER_ID);
        Course course = course(UUID.randomUUID(), OTHER_USER_ID, true, 4);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(courseRepository.findByIdAndIsDeletedFalse(course.getId())).willReturn(Optional.of(course));

        runningRecordService.createRecord(USER_ID, request(course.getId(), null));

        ArgumentCaptor<RunningRecord> recordCaptor = ArgumentCaptor.forClass(RunningRecord.class);
        verify(runningRecordRepository).save(recordCaptor.capture());
        RunningRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getCourse()).isEqualTo(course);
        assertThat(savedRecord.getRunningMode()).isEqualTo(RunningMode.COURSE_SELECT);
        assertThat(course.getCompletionCount()).isEqualTo(5);
    }

    @Test
    void createsCourseCreateRecordWhenRequested() {
        User user = user(USER_ID);
        Course course = course(UUID.randomUUID(), USER_ID, false, 0);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(courseRepository.findByIdAndIsDeletedFalse(course.getId())).willReturn(Optional.of(course));

        runningRecordService.createRecord(USER_ID, request(course.getId(), RunningMode.COURSE_CREATE));

        ArgumentCaptor<RunningRecord> recordCaptor = ArgumentCaptor.forClass(RunningRecord.class);
        verify(runningRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getRunningMode()).isEqualTo(RunningMode.COURSE_CREATE);
        assertThat(recordCaptor.getValue().getCourse()).isEqualTo(course);
        assertThat(course.getCompletionCount()).isEqualTo(1);
    }

    @Test
    void rejectsPrivateCourseOwnedByOtherUser() {
        User user = user(USER_ID);
        Course course = course(UUID.randomUUID(), OTHER_USER_ID, false, 2);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(courseRepository.findByIdAndIsDeletedFalse(course.getId())).willReturn(Optional.of(course));

        assertThatThrownBy(() -> runningRecordService.createRecord(
                USER_ID,
                request(course.getId(), RunningMode.COURSE_SELECT)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("연결할 코스를 찾을 수 없습니다.");

        verify(runningRecordRepository, never()).save(any());
        assertThat(course.getCompletionCount()).isEqualTo(2);
    }

    @Test
    void rejectsCourseRunningModeWithoutCourse() {
        User user = user(USER_ID);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> runningRecordService.createRecord(
                USER_ID,
                request(null, RunningMode.COURSE_SELECT)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("코스 러닝 모드에는 코스가 필요합니다.");

        verify(runningRecordRepository, never()).save(any());
    }

    private static CreateRunningRecordRequest request(UUID courseId, RunningMode runningMode) {
        return new CreateRunningRecordRequest(
                List.of(
                        new CreateRunningRecordRequest.RoutePoint(33.4996, 126.5312),
                        new CreateRunningRecordRequest.RoutePoint(33.5100, 126.5400)
                ),
                3_200,
                1_920,
                6.0,
                180,
                STARTED_AT,
                ENDED_AT,
                courseId,
                runningMode
        );
    }

    private static User user(UUID id) {
        User user = User.createKakaoUser("kakao-" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Course course(UUID id, UUID creatorId, boolean isPublic, int completionCount) {
        var route = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(126.5312, 33.4996),
                new Coordinate(126.5400, 33.5100)
        });
        route.setSRID(4326);
        var startPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(126.5312, 33.4996));
        startPoint.setSRID(4326);
        Course course = Course.create(
                user(creatorId),
                "제주 러닝 코스",
                "제주를 달리는 코스",
                CourseType.RUNNING_COURSE,
                new BigDecimal("3.20"),
                32,
                BigDecimal.ZERO,
                Difficulty.LOW,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                route,
                startPoint,
                null,
                isPublic
        );
        ReflectionTestUtils.setField(course, "id", id);
        ReflectionTestUtils.setField(course, "completionCount", completionCount);
        return course;
    }
}
