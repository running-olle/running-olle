package com.runningolle.domain.mypage.service;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseBookmark;
import com.runningolle.domain.course.dto.CourseWaypointResponse;
import com.runningolle.domain.course.dto.RouteCoordinateResponse;
import com.runningolle.domain.course.repository.CourseBookmarkRepository;
import com.runningolle.domain.course.repository.CourseWaypointRepository;
import com.runningolle.domain.mypage.dto.MyPageDtos;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.entity.RunningWaypointVisit;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import com.runningolle.domain.running.repository.RunningWaypointVisitRepository;
import com.runningolle.domain.trip.entity.Trip;
import com.runningolle.domain.trip.repository.TripRepository;
import com.runningolle.domain.user.entity.Theme;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.entity.UserNotificationSetting;
import com.runningolle.domain.user.entity.UserTheme;
import com.runningolle.domain.user.entity.UserType;
import com.runningolle.domain.user.enums.AccountStatus;
import com.runningolle.domain.user.enums.UserTypeCode;
import com.runningolle.domain.user.repository.ThemeRepository;
import com.runningolle.domain.user.repository.UserNotificationSettingRepository;
import com.runningolle.domain.user.repository.UserRepository;
import com.runningolle.domain.user.repository.UserThemeRepository;
import com.runningolle.domain.user.repository.UserTypeRepository;
import com.runningolle.domain.user.repository.UserUserTypeRepository;
import com.runningolle.domain.user.entity.UserUserType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
    private final ThemeRepository themeRepository;
    private final UserThemeRepository userThemeRepository;
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
        if (nickname.length() < 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임을 2자 이상 입력해주세요.");
        if (userRepository.existsByNickname(nickname) && !nickname.equals(user.getNickname()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        user.updateProfile(nickname, request.profileImageUrl(), trim(request.bio()), request.preferredDistance(), request.preferredDifficulty());

        List<String> requestedTypes = validateUserTypes(request.userTypes());
        List<String> currentTypes = userUserTypeRepository.findAllByUserId(userId).stream()
                .map(link -> link.getUserType().getCode())
                .toList();
        if (!new LinkedHashSet<>(currentTypes).equals(new LinkedHashSet<>(requestedTypes))) {
            userUserTypeRepository.deleteAllByUserId(userId);
            // Hibernate executes inserts before entity deletes unless the delete queue is flushed first.
            // Without this flush, re-selecting a profile type can violate the user/type unique constraint.
            userUserTypeRepository.flush();
            for (String code : requestedTypes) {
                UserTypeCode value = UserTypeCode.valueOf(code);
                UserType type = userTypeRepository.findByCode(code)
                        .orElseGet(() -> userTypeRepository.save(UserType.of(code, value.getDisplayName())));
                userUserTypeRepository.save(UserUserType.of(user, type));
            }
        }
        if (request.themeIds() != null) {
            syncUserThemes(user, request.themeIds());
        }
        return profile(userId);
    }

    @Transactional(readOnly = true)
    public List<MyPageDtos.Run> runs(UUID userId) {
        activeUser(userId);
        return runningRecordRepository.findAllByUserIdOrderByStartedAtDesc(userId).stream().map(this::runDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MyPageDtos.Visit> visits(UUID userId) {
        activeUser(userId);
        return visitRepository.findAllByRunningRecordUserIdOrderByVisitedAtDesc(userId).stream()
                .map(this::visitDto)
                .toList();
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
    public List<MyPageDtos.RunTripReportSummary> reports(UUID userId) {
        activeUser(userId);
        return tripRepository.findAllByUserIdOrderByStartDateDesc(userId).stream()
                .map(this::reportSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public MyPageDtos.RunTripReportDetail report(UUID userId, UUID reportId) {
        activeUser(userId);
        Trip report = reportForUser(userId, reportId);
        List<RunningRecord> runs = runsFor(userId, report.getStartDate(), report.getEndDate());
        List<RunningWaypointVisit> visits = visitsFor(userId, report.getStartDate(), report.getEndDate());
        return reportDetail(report, runs, visits);
    }

    @Transactional(readOnly = true)
    public MyPageDtos.RunTripReportStatistics reportPreview(UUID userId, LocalDate startDate, LocalDate endDate) {
        activeUser(userId);
        validateReport("preview", startDate, endDate);
        return reportStatistics(runsFor(userId, startDate, endDate), visitsFor(userId, startDate, endDate));
    }

    @Transactional(readOnly = true)
    public MyPageDtos.RunTripOverallStatistics overallReportStatistics(UUID userId) {
        activeUser(userId);
        List<Trip> reports = tripRepository.findAllByUserIdOrderByStartDateDesc(userId);
        Map<UUID, RunningRecord> uniqueRuns = new LinkedHashMap<>();
        Set<UUID> uniquePlaces = new LinkedHashSet<>();
        BigDecimal summedReportDistance = BigDecimal.ZERO;

        for (Trip report : reports) {
            List<RunningRecord> reportRuns = runsFor(userId, report.getStartDate(), report.getEndDate());
            reportRuns.forEach(run -> uniqueRuns.putIfAbsent(run.getId(), run));
            summedReportDistance = summedReportDistance.add(totalDistance(reportRuns));
            visitsFor(userId, report.getStartDate(), report.getEndDate()).stream()
                    .map(visit -> visit.getCourseWaypoint().getId())
                    .forEach(uniquePlaces::add);
        }

        List<RunningRecord> runs = new ArrayList<>(uniqueRuns.values());
        BigDecimal distance = totalDistance(runs);
        long duration = totalDuration(runs);
        BigDecimal averageDistance = reports.isEmpty()
                ? BigDecimal.ZERO
                : summedReportDistance.divide(BigDecimal.valueOf(reports.size()), 2, RoundingMode.HALF_UP);
        return new MyPageDtos.RunTripOverallStatistics(
                reports.size(), runs.size(), uniqueCourseCount(runs), distance, duration,
                averagePace(distance, duration), uniquePlaces.size(), averageDistance
        );
    }

    @Transactional
    public MyPageDtos.RunTripReportDetail createReport(UUID userId, MyPageDtos.SaveRunTripReportRequest request) {
        User user = activeUser(userId);
        validateReport(request.name(), request.startDate(), request.endDate());
        Trip report = tripRepository.save(Trip.create(
                user, request.name().trim(), null, request.startDate(), request.endDate(), trim(request.thumbnailImageUrl())
        ));
        return reportDetail(report, runsFor(userId, report.getStartDate(), report.getEndDate()),
                visitsFor(userId, report.getStartDate(), report.getEndDate()));
    }

    @Transactional
    public MyPageDtos.RunTripReportDetail updateReport(UUID userId, UUID reportId, MyPageDtos.SaveRunTripReportRequest request) {
        activeUser(userId);
        validateReport(request.name(), request.startDate(), request.endDate());
        Trip report = reportForUser(userId, reportId);
        report.update(request.name().trim(), null, request.startDate(), request.endDate(), trim(request.thumbnailImageUrl()));
        return reportDetail(report, runsFor(userId, report.getStartDate(), report.getEndDate()),
                visitsFor(userId, report.getStartDate(), report.getEndDate()));
    }

    @Transactional
    public void deleteReport(UUID userId, UUID reportId) {
        activeUser(userId);
        Trip report = reportForUser(userId, reportId);
        runningRecordRepository.findAllByTripIdOrderByStartedAtDesc(reportId)
                .forEach(record -> record.assignToTrip(null));
        runningRecordRepository.flush();
        tripRepository.delete(report);
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
    private MyPageDtos.Visit visitDto(RunningWaypointVisit visit) {
        var waypoint = visit.getCourseWaypoint();
        Course course = waypoint.getCourse();
        String imageUrl = visit.getPhotoUrl() != null ? visit.getPhotoUrl() : course.getThumbnailImageUrl();
        return new MyPageDtos.Visit(
                visit.getId(), waypoint.getId(), course.getId(), course.getName(), waypoint.getName(),
                waypoint.getDescription(), imageUrl, visit.getVisitedAt(), waypoint.getOrderIndex(),
                waypoint.getLocation().getY(), waypoint.getLocation().getX()
        );
    }
    private MyPageDtos.RunTripReportSummary reportSummary(Trip report) {
        return new MyPageDtos.RunTripReportSummary(report.getId(), report.getName(), report.getStartDate(),
                report.getEndDate(), report.getThumbnailImageUrl());
    }

    private MyPageDtos.RunTripReportDetail reportDetail(Trip report, List<RunningRecord> runs,
                                                         List<RunningWaypointVisit> visits) {
        long runningCourseRuns = runs.stream().filter(run -> run.getCourse() != null
                && run.getCourse().getCourseType() == com.runningolle.domain.course.enums.CourseType.RUNNING_COURSE).count();
        long spotCourseRuns = runs.stream().filter(run -> run.getCourse() != null
                && run.getCourse().getCourseType() == com.runningolle.domain.course.enums.CourseType.SPOT_COURSE).count();
        long freeRuns = runs.size() - runningCourseRuns - spotCourseRuns;
        return new MyPageDtos.RunTripReportDetail(
                report.getId(), report.getName(), report.getStartDate(), report.getEndDate(), report.getThumbnailImageUrl(),
                reportStatistics(runs, visits),
                new MyPageDtos.RunTripReportBreakdown(runningCourseRuns, spotCourseRuns, freeRuns),
                runs.stream().map(this::runDto).toList(), visits.stream().map(this::visitDto).toList()
        );
    }

    private MyPageDtos.RunTripReportStatistics reportStatistics(List<RunningRecord> runs,
                                                                 List<RunningWaypointVisit> visits) {
        BigDecimal distance = totalDistance(runs);
        long duration = totalDuration(runs);
        long uniquePlaces = visits.stream().map(visit -> visit.getCourseWaypoint().getId()).distinct().count();
        return new MyPageDtos.RunTripReportStatistics(runs.size(), uniqueCourseCount(runs), distance, duration,
                averagePace(distance, duration), uniquePlaces);
    }

    private List<RunningRecord> runsFor(UUID userId, LocalDate startDate, LocalDate endDate) {
        return runningRecordRepository
                .findAllByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
                        userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private List<RunningWaypointVisit> visitsFor(UUID userId, LocalDate startDate, LocalDate endDate) {
        return visitRepository
                .findAllByRunningRecordUserIdAndRunningRecordStartedAtGreaterThanEqualAndRunningRecordStartedAtLessThanOrderByVisitedAtDesc(
                        userId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private BigDecimal totalDistance(List<RunningRecord> runs) {
        return runs.stream().map(RunningRecord::getTotalDistanceKm).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long totalDuration(List<RunningRecord> runs) {
        return runs.stream().mapToLong(RunningRecord::getTotalDurationSeconds).sum();
    }

    private long uniqueCourseCount(List<RunningRecord> runs) {
        return runs.stream().map(RunningRecord::getCourse).filter(java.util.Objects::nonNull)
                .map(Course::getId).distinct().count();
    }

    private BigDecimal averagePace(BigDecimal distance, long durationSeconds) {
        if (distance == null || distance.signum() <= 0 || durationSeconds <= 0) return null;
        return BigDecimal.valueOf(durationSeconds)
                .divide(distance.multiply(BigDecimal.valueOf(60)), 2, RoundingMode.HALF_UP);
    }

    private Trip reportForUser(UUID userId, UUID reportId) {
        return tripRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "런트립 리포트를 찾을 수 없습니다."));
    }

    private void validateReport(String name, LocalDate startDate, LocalDate endDate) {
        if (name == null || name.isBlank() || startDate == null || endDate == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "여행 이름과 기간을 입력해주세요.");
        if (endDate.isBefore(startDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
    }
    private MyPageDtos.NotificationSettings notificationDto(UserNotificationSetting s) {
        return new MyPageDtos.NotificationSettings(s.getRecommendedCourse(), s.getWeather(), s.getSavedCourseUpdate(),
                s.getMeetupInvite(), s.getCommentLike(), s.getTierChange(), s.getEventChallenge());
    }
    private void syncUserThemes(User user, List<UUID> themeIds) {
        userThemeRepository.deleteAllByUserId(user.getId());
        List<UUID> distinctThemeIds = distinctIds(themeIds);
        if (distinctThemeIds.isEmpty()) {
            return;
        }

        List<Theme> themes = themeRepository.findAllById(distinctThemeIds);
        if (themes.size() != distinctThemeIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some selected themes do not exist.");
        }

        List<UserTheme> userThemes = new ArrayList<>(themes.size());
        for (Theme theme : themes) {
            userThemes.add(UserTheme.of(user, theme));
        }
        userThemeRepository.saveAll(userThemes);
    }

    private List<UUID> distinctIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<UUID> distinctIds = new LinkedHashSet<>();
        for (UUID id : ids) {
            if (id == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Theme id is required.");
            }
            distinctIds.add(id);
        }
        return new ArrayList<>(distinctIds);
    }

    private User activeUser(UUID id) { return userRepository.findById(id).filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.")); }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private List<String> validateUserTypes(List<String> userTypes) {
        if (userTypes == null) return List.of();
        try {
            return userTypes.stream()
                    .map(UserTypeCode::valueOf)
                    .distinct()
                    .map(Enum::name)
                    .toList();
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 사용자 유형을 선택해주세요.");
        }
    }
}
