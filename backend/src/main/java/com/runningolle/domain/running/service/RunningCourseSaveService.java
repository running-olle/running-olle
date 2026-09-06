package com.runningolle.domain.running.service;

import com.runningolle.domain.course.dto.CourseCreateResponse;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseWaypoint;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseWaypointRepository;
import com.runningolle.domain.running.dto.SaveRunningCourseRequest;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.enums.RunningMode;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RunningCourseSaveService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final RunningRecordRepository runningRecordRepository;
    private final CourseRepository courseRepository;
    private final CourseWaypointRepository courseWaypointRepository;

    @Transactional
    public CourseCreateResponse saveAsCourse(UUID userId, UUID recordId, SaveRunningCourseRequest request) {
        RunningRecord record = runningRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "러닝 기록을 찾을 수 없습니다."));
        if (record.getCourse() != null || record.getRunningMode() != RunningMode.FREE_RUN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 코스가 연결된 러닝 기록입니다.");
        }

        LineString route = (LineString) record.getRoute().copy();
        route.setSRID(4326);
        if (route.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "저장할 러닝 경로가 없습니다.");
        }

        Coordinate startCoordinate = route.getCoordinateN(0);
        Coordinate endCoordinate = route.getCoordinateN(route.getNumPoints() - 1);
        Course course = courseRepository.save(Course.create(
                record.getUser(),
                request.name().trim(),
                trimToNull(request.description()),
                CourseType.RUNNING_COURSE,
                record.getTotalDistanceKm(),
                Math.max(1, (int) Math.ceil(record.getTotalDurationSeconds() / 60.0)),
                record.getElevationGainM() == null ? ZERO : record.getElevationGainM(),
                Difficulty.LOW,
                ZERO,
                ZERO,
                ZERO,
                route,
                point(startCoordinate),
                null,
                request.isPublic() == null || request.isPublic()
        ));
        course.increaseCompletionCount();

        courseWaypointRepository.saveAll(List.of(
                CourseWaypoint.create(course, "출발점", null, point(startCoordinate), 0, ZERO,
                        null, null, null, null, null),
                CourseWaypoint.create(course, "도착점", null, point(endCoordinate), 1,
                        record.getTotalDistanceKm(), null, null, null, null, null)
        ));
        record.attachCreatedCourse(course);

        return new CourseCreateResponse(course.getId());
    }

    private static Point point(Coordinate coordinate) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(coordinate));
        point.setSRID(4326);
        return point;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
