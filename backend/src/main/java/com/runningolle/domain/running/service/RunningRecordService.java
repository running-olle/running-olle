package com.runningolle.domain.running.service;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.running.dto.CreateRunningRecordRequest;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.enums.RunningMode;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.enums.AccountStatus;
import com.runningolle.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RunningRecordService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final RunningRecordRepository runningRecordRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public UUID createRecord(UUID userId, CreateRunningRecordRequest request) {
        User user = userRepository.findById(userId)
                .filter(found -> found.getAccountStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (request.endedAt().isBefore(request.startedAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료 시각은 시작 시각보다 빠를 수 없습니다.");
        }

        Course course = findVisibleCourse(request.courseId(), userId);
        RunningMode runningMode = resolveRunningMode(request.runningMode(), course);
        LineString route = toLineString(request.route());
        BigDecimal totalDistanceKm = BigDecimal.valueOf(request.totalDistanceMeters() / 1_000)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal calories = BigDecimal.valueOf(request.calories()).setScale(2, RoundingMode.HALF_UP);
        LocalDateTime startedAt = LocalDateTime.ofInstant(request.startedAt().toInstant(), ZoneOffset.UTC);
        LocalDateTime endedAt = LocalDateTime.ofInstant(request.endedAt().toInstant(), ZoneOffset.UTC);

        RunningRecord record = course == null
                ? RunningRecord.createFreeRun(
                        user,
                        route,
                        totalDistanceKm,
                        request.totalDurationSeconds(),
                        decimalOrNull(request.averagePace()),
                        calories,
                        startedAt,
                        endedAt
                )
                : RunningRecord.createCourseRun(
                        user,
                        course,
                        runningMode,
                        route,
                        totalDistanceKm,
                        request.totalDurationSeconds(),
                        decimalOrNull(request.averagePace()),
                        calories,
                        startedAt,
                        endedAt
                );
        if (course != null) {
            course.increaseCompletionCount();
        }
        return runningRecordRepository.save(record).getId();
    }

    private Course findVisibleCourse(UUID courseId, UUID userId) {
        if (courseId == null) {
            return null;
        }
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "연결할 코스를 찾을 수 없습니다."));
        if (!Boolean.TRUE.equals(course.getIsPublic()) && !course.getCreator().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "연결할 코스를 찾을 수 없습니다.");
        }
        return course;
    }

    private RunningMode resolveRunningMode(RunningMode requestedMode, Course course) {
        if (course == null) {
            if (requestedMode != null && requestedMode != RunningMode.FREE_RUN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "코스 러닝 모드에는 코스가 필요합니다.");
            }
            return RunningMode.FREE_RUN;
        }
        if (requestedMode == null) {
            return RunningMode.COURSE_SELECT;
        }
        if (requestedMode == RunningMode.FREE_RUN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "코스가 있는 기록은 자유 달리기로 저장할 수 없습니다.");
        }
        return requestedMode;
    }

    private LineString toLineString(List<CreateRunningRecordRequest.RoutePoint> points) {
        List<Coordinate> coordinates = new ArrayList<>(points.stream()
                .map(point -> new Coordinate(point.longitude(), point.latitude()))
                .toList());
        if (coordinates.size() == 1) coordinates.add(new Coordinate(coordinates.get(0)));
        return GEOMETRY_FACTORY.createLineString(coordinates.toArray(Coordinate[]::new));
    }

    private BigDecimal decimalOrNull(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
