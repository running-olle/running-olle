package com.runningolle.domain.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseWaypoint;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.course.repository.CourseBookmarkRepository;
import com.runningolle.domain.course.repository.CourseWaypointRepository;
import com.runningolle.domain.mypage.dto.MyPageDtos;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.enums.RunningMode;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import com.runningolle.domain.running.repository.RunningWaypointVisitRepository;
import com.runningolle.domain.trip.repository.TripRepository;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.entity.UserType;
import com.runningolle.domain.user.entity.UserUserType;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import com.runningolle.domain.user.repository.UserNotificationSettingRepository;
import com.runningolle.domain.user.repository.UserRepository;
import com.runningolle.domain.user.repository.UserTypeRepository;
import com.runningolle.domain.user.repository.UserUserTypeRepository;
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
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserUserTypeRepository userUserTypeRepository;

    @Mock
    private UserTypeRepository userTypeRepository;

    @Mock
    private UserNotificationSettingRepository notificationRepository;

    @Mock
    private RunningRecordRepository runningRecordRepository;

    @Mock
    private RunningWaypointVisitRepository visitRepository;

    @Mock
    private CourseBookmarkRepository bookmarkRepository;

    @Mock
    private CourseWaypointRepository courseWaypointRepository;

    @Mock
    private TripRepository tripRepository;

    private MyPageService myPageService;

    @BeforeEach
    void setUp() {
        myPageService = new MyPageService(
                userRepository,
                userUserTypeRepository,
                userTypeRepository,
                notificationRepository,
                runningRecordRepository,
                visitRepository,
                bookmarkRepository,
                courseWaypointRepository,
                tripRepository
        );
    }

    @Test
    void returnsRunDetailWithRecordedRouteCourseRouteAndWaypoints() {
        User user = user(USER_ID);
        Course course = course(UUID.randomUUID(), USER_ID);
        RunningRecord record = runningRecord(UUID.randomUUID(), user, course);
        CourseWaypoint firstWaypoint = waypoint(course, "성산일출봉", 0);
        CourseWaypoint secondWaypoint = waypoint(course, "광치기해변", 1);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(runningRecordRepository.findByIdAndUserId(record.getId(), USER_ID)).willReturn(Optional.of(record));
        given(courseWaypointRepository.findByCourse_IdOrderByOrderIndexAsc(course.getId()))
                .willReturn(List.of(firstWaypoint, secondWaypoint));

        MyPageDtos.RunDetail detail = myPageService.runDetail(USER_ID, record.getId());

        assertThat(detail.id()).isEqualTo(record.getId());
        assertThat(detail.courseId()).isEqualTo(course.getId());
        assertThat(detail.courseName()).isEqualTo("제주 러닝 코스");
        assertThat(detail.runningMode()).isEqualTo(RunningMode.COURSE_SELECT);
        assertThat(detail.recordedRouteCoordinates()).hasSize(3);
        assertThat(detail.plannedRouteCoordinates()).hasSize(2);
        assertThat(detail.courseWaypoints()).extracting("name").containsExactly("성산일출봉", "광치기해변");
    }

    @Test
    void throwsNotFoundWhenRunDoesNotBelongToUser() {
        User user = user(USER_ID);
        UUID runId = UUID.randomUUID();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(runningRecordRepository.findByIdAndUserId(runId, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> myPageService.runDetail(USER_ID, runId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("러닝 기록을 찾을 수 없습니다.");
    }

    @Test
    void updatesProfileWithoutRecreatingUnchangedUserType() {
        User user = user(USER_ID);
        user.updateProfile("기존닉네임", null, null, null, null);
        UserType activeRunner = UserType.of("ACTIVE_RUNNER", "활동적인 러너");
        UserUserType currentLink = UserUserType.of(user, activeRunner);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(USER_ID)).willReturn(List.of(currentLink));

        MyPageDtos.Profile profile = myPageService.updateProfile(USER_ID, new MyPageDtos.UpdateProfileRequest(
                "  새닉네임  ", null, "  새로운 소개  ", List.of("ACTIVE_RUNNER"),
                PreferredDistance.FROM_5_TO_10KM, PreferredDifficulty.NORMAL
        ));

        assertThat(profile.nickname()).isEqualTo("새닉네임");
        assertThat(profile.bio()).isEqualTo("새로운 소개");
        assertThat(profile.userTypes()).containsExactly("ACTIVE_RUNNER");
        verify(userUserTypeRepository, never()).deleteAllByUserId(USER_ID);
        verify(userUserTypeRepository, never()).flush();
        verify(userUserTypeRepository, never()).save(any(UserUserType.class));
    }

    @Test
    void flushesDeletedUserTypesBeforeSavingReplacement() {
        User user = user(USER_ID);
        user.updateProfile("기존닉네임", null, null, null, null);
        UserUserType currentLink = UserUserType.of(user, UserType.of("ACTIVE_RUNNER", "활동적인 러너"));
        UserType relaxedTraveler = UserType.of("RELAXED_TRAVELER", "여유로운 여행자");
        UserUserType replacementLink = UserUserType.of(user, relaxedTraveler);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(userUserTypeRepository.findAllByUserId(USER_ID))
                .willReturn(List.of(currentLink), List.of(replacementLink));
        given(userTypeRepository.findByCode("RELAXED_TRAVELER")).willReturn(Optional.of(relaxedTraveler));

        myPageService.updateProfile(USER_ID, new MyPageDtos.UpdateProfileRequest(
                "기존닉네임", null, null, List.of("RELAXED_TRAVELER"), null, null
        ));

        InOrder order = inOrder(userUserTypeRepository);
        order.verify(userUserTypeRepository).deleteAllByUserId(USER_ID);
        order.verify(userUserTypeRepository).flush();
        order.verify(userUserTypeRepository).save(any(UserUserType.class));
    }

    @Test
    void rejectsUnknownUserType() {
        User user = user(USER_ID);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> myPageService.updateProfile(USER_ID, new MyPageDtos.UpdateProfileRequest(
                "새닉네임", null, null, List.of("UNKNOWN"), null, null
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("올바른 사용자 유형");
    }

    private static RunningRecord runningRecord(UUID id, User user, Course course) {
        var route = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(126.5312, 33.4996),
                new Coordinate(126.5400, 33.5100),
                new Coordinate(126.5500, 33.5200)
        });
        route.setSRID(4326);
        RunningRecord record = RunningRecord.createCourseRun(
                user,
                course,
                RunningMode.COURSE_SELECT,
                route,
                new BigDecimal("3.20"),
                1_920,
                new BigDecimal("6.00"),
                new BigDecimal("180.00"),
                LocalDateTime.of(2026, 8, 29, 7, 0),
                LocalDateTime.of(2026, 8, 29, 7, 32)
        );
        ReflectionTestUtils.setField(record, "id", id);
        return record;
    }

    private static Course course(UUID id, UUID creatorId) {
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
                "성산 바다를 보며 달리는 코스",
                CourseType.RUNNING_COURSE,
                new BigDecimal("3.20"),
                32,
                new BigDecimal("12.00"),
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
        return course;
    }

    private static CourseWaypoint waypoint(Course course, String name, int orderIndex) {
        var point = GEOMETRY_FACTORY.createPoint(new Coordinate(126.5312 + orderIndex * 0.01, 33.4996 + orderIndex * 0.01));
        point.setSRID(4326);
        return CourseWaypoint.create(
                course,
                name,
                null,
                point,
                orderIndex,
                new BigDecimal(Integer.toString(orderIndex)),
                null,
                null,
                null,
                null,
                null
        );
    }

    private static User user(UUID id) {
        User user = User.createKakaoUser("kakao-" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
