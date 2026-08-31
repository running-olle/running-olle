package com.runningolle.domain.mypage.service;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseBookmark;
import com.runningolle.domain.course.dto.CourseWaypointResponse;
import com.runningolle.domain.course.dto.RouteCoordinateResponse;
import com.runningolle.domain.course.repository.CourseBookmarkRepository;
import com.runningolle.domain.course.repository.CourseWaypointRepository;
import com.runningolle.domain.mypage.dto.MyPageDtos;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import com.runningolle.domain.running.repository.RunningWaypointVisitRepository;
import com.runningolle.domain.trip.entity.Trip;
import com.runningolle.domain.trip.repository.TripRepository;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.entity.UserNotificationSetting;
import com.runningolle.domain.user.entity.UserType;
import com.runningolle.domain.user.enums.AccountStatus;
import com.runningolle.domain.user.enums.UserTypeCode;
import com.runningolle.domain.user.repository.UserNotificationSettingRepository;
import com.runningolle.domain.user.repository.UserRepository;
import com.runningolle.domain.user.repository.UserTypeRepository;
import com.runningolle.domain.user.repository.UserUserTypeRepository;
import com.runningolle.domain.user.entity.UserUserType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MyPageService {
    private final UserRepository userRepository;
    private final UserUserTypeRepository userUserTypeRepository;
    private final UserTypeRepository userTypeRepository;
    private final UserNotificationSettingRepository notificationRepository;
    private final RunningRecordRepository runningRecordRepository;
    private final RunningWaypointVisitRepository visitRepository;
    private final CourseBookmarkRepository bookmarkRepository;
    private final CourseWaypointRepository courseWaypointRepository;
    private final TripRepository tripRepository;

    @Transactional(readOnly = true)
    public MyPageDtos.Dashboard dashboard(UUID userId) {
        List<RunningRecord> runs = runningRecordRepository.findAllByUserIdOrderByStartedAtDesc(userId);
        BigDecimal distance = runs.stream().map(RunningRecord::getTotalDistanceKm).reduce(BigDecimal.ZERO, BigDecimal::add);
        long unique = runs.stream().map(RunningRecord::getCourse).filter(java.util.Objects::nonNull).map(Course::getId).distinct().count();
        return new MyPageDtos.Dashboard(profile(userId), distance, runs.size(), unique);
    }

    @Transactional(readOnly = true)
    public MyPageDtos.Profile profile(UUID userId) {
        User user = activeUser(userId);
        List<String> types = userUserTypeRepository.findAllByUserId(userId).stream().map(x -> x.getUserType().getCode()).toList();
        return new MyPageDtos.Profile(user.getNickname(), user.getProfileImageUrl(), user.getBio(), types,
                user.getPreferredDistance(), user.getPreferredDifficulty(), user.getCreatedAt(), user.getAccountStatus().name());
    }

    @Transactional
    public MyPageDtos.Profile updateProfile(UUID userId, MyPageDtos.UpdateProfileRequest request) {
        User user = activeUser(userId);
        String nickname = request.nickname() == null ? "" : request.nickname().trim();
        if (nickname.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임을 입력해주세요.");
        if (userRepository.existsByNickname(nickname) && !nickname.equals(user.getNickname()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        user.updateProfile(nickname, request.profileImageUrl(), trim(request.bio()), request.preferredDistance(), request.preferredDifficulty());
        userUserTypeRepository.deleteAllByUserId(userId);
        if (request.userTypes() != null) for (String code : request.userTypes()) {
            UserTypeCode value = UserTypeCode.valueOf(code);
            UserType type = userTypeRepository.findByCode(code).orElseGet(() -> userTypeRepository.save(UserType.of(code, value.getDisplayName())));
            userUserTypeRepository.save(UserUserType.of(user, type));
        }
        return profile(userId);
    }

    @Transactional(readOnly = true)
    public List<MyPageDtos.Run> runs(UUID userId) {
        activeUser(userId);
        return runningRecordRepository.findAllByUserIdOrderByStartedAtDesc(userId).stream().map(this::runDto).toList();
    }

    @Transactional(readOnly = true)
    public MyPageDtos.RunDetail runDetail(UUID userId, UUID runId) {
        activeUser(userId);
        RunningRecord record = runningRecordRepository.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "러닝 기록을 찾을 수 없습니다."));
        Course course = record.getCourse();
        List<CourseWaypointResponse> waypoints = course == null
                ? List.of()
                : courseWaypointRepository.findByCourse_IdOrderByOrderIndexAsc(course.getId())
                .stream()
                .map(CourseWaypointResponse::from)
                .toList();

        return new MyPageDtos.RunDetail(
                record.getId(),
                course == null ? null : course.getId(),
                course == null ? null : course.getName(),
                course == null ? null : course.getDescription(),
                course == null ? null : course.getCourseType(),
                course == null ? null : course.getDifficulty(),
                course == null ? null : course.getThumbnailImageUrl(),
                record.getRunningMode(),
                record.getTotalDistanceKm(),
                record.getTotalDurationSeconds(),
                record.getAvgPace(),
                record.getCalories(),
                record.getElevationGainM(),
                record.getStartedAt(),
                record.getEndedAt(),
                RouteCoordinateResponse.from(record.getRoute()),
                course == null ? List.of() : RouteCoordinateResponse.from(course.getRoute()),
                waypoints
        );
    }

    @Transactional(readOnly = true)
    public List<MyPageDtos.Bookmark> bookmarks(UUID userId) {
        activeUser(userId);
        return bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(bookmark -> !Boolean.TRUE.equals(bookmark.getCourse().getIsDeleted()))
                .map(bookmark -> {
                    Course c = bookmark.getCourse();
                    return new MyPageDtos.Bookmark(
                            bookmark.getId(),
                            c.getId(),
                            c.getName(),
                            c.getCourseType(),
                            c.getDistanceKm(),
                            c.getDifficulty(),
                            c.getThumbnailImageUrl(),
                            c.getCreator().getId().equals(userId)
                    );
                }).toList();
    }

    @Transactional
    public void deleteBookmark(UUID userId, UUID bookmarkId) {
        CourseBookmark bookmark = bookmarkRepository.findByIdAndUserId(bookmarkId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저장한 코스를 찾을 수 없습니다."));
        bookmarkRepository.delete(bookmark);
    }

    @Transactional(readOnly = true)
    public List<MyPageDtos.TripResponse> trips(UUID userId) {
        activeUser(userId);
        return tripRepository.findAllByUserIdOrderByStartDateDesc(userId).stream().map(this::tripDto).toList();
    }

    @Transactional
    public MyPageDtos.TripResponse createTrip(UUID userId, MyPageDtos.CreateTripRequest request) {
        User user = activeUser(userId);
        if (request.name() == null || request.name().isBlank() || request.startDate() == null || request.endDate() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "여행명과 기간을 입력해주세요.");
        if (request.endDate().isBefore(request.startDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        Trip trip = tripRepository.save(Trip.create(user, request.name().trim(), trim(request.region()), request.startDate(), request.endDate(), request.thumbnailImageUrl()));
        runningRecordRepository.findAllByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                userId, request.startDate().atStartOfDay(), request.endDate().plusDays(1).atStartOfDay()
        ).forEach(record -> record.assignToTrip(trip));
        runningRecordRepository.flush();
        return tripDto(trip);
    }

    @Transactional(readOnly = true)
    public MyPageDtos.NotificationSettings notifications(UUID userId) {
        activeUser(userId);
        return notificationRepository.findByUserId(userId).map(this::notificationDto)
                .orElse(new MyPageDtos.NotificationSettings(true, true, true, true, true, true, true));
    }

    @Transactional
    public MyPageDtos.NotificationSettings updateNotifications(UUID userId, MyPageDtos.NotificationSettings request) {
        User user = activeUser(userId);
        UserNotificationSetting setting = notificationRepository.findByUserId(userId).orElseGet(() ->
                notificationRepository.save(UserNotificationSetting.create(user, false, true, true, true, true)));
        setting.update(request.recommendedCourse(), request.weather(), request.savedCourseUpdate(), request.meetupInvite(),
                request.commentLike(), request.tierChange(), request.eventChallenge());
        return notificationDto(setting);
    }

    private MyPageDtos.Run runDto(RunningRecord r) {
        Course c = r.getCourse();
        return new MyPageDtos.Run(r.getId(), c == null ? null : c.getId(), c == null ? null : c.getName(),
                c == null ? null : c.getCourseType(), c == null ? null : c.getThumbnailImageUrl(), r.getTotalDistanceKm(),
                r.getTotalDurationSeconds(), r.getAvgPace(), r.getStartedAt());
    }
    private MyPageDtos.TripResponse tripDto(Trip trip) {
        List<RunningRecord> runs = runningRecordRepository.findAllByTripIdOrderByStartedAtDesc(trip.getId());
        BigDecimal distance = runs.stream().map(RunningRecord::getTotalDistanceKm).reduce(BigDecimal.ZERO, BigDecimal::add);
        long duration = runs.stream().mapToLong(RunningRecord::getTotalDurationSeconds).sum();
        return new MyPageDtos.TripResponse(trip.getId(), trip.getName(), trip.getRegion(), trip.getStartDate(), trip.getEndDate(),
                trip.getThumbnailImageUrl(), runs.size(), distance, visitRepository.countByRunningRecordTripId(trip.getId()), duration);
    }
    private MyPageDtos.NotificationSettings notificationDto(UserNotificationSetting s) {
        return new MyPageDtos.NotificationSettings(s.getRecommendedCourse(), s.getWeather(), s.getSavedCourseUpdate(),
                s.getMeetupInvite(), s.getCommentLike(), s.getTierChange(), s.getEventChallenge());
    }
    private User activeUser(UUID id) { return userRepository.findById(id).filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")); }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
