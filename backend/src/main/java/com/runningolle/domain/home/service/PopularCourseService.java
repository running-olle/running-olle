package com.runningolle.domain.home.service;

import com.runningolle.domain.home.dto.PopularCoursesResponse;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PopularCourseService {

    private static final int POPULAR_COURSE_LIMIT = 5;
    private static final int POPULAR_PERIOD_DAYS = 30;

    private final RunningRecordRepository runningRecordRepository;

    @Transactional(readOnly = true)
    public PopularCoursesResponse getPopularCourses() {
        LocalDateTime endedAt = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime startedAt = endedAt.minusDays(POPULAR_PERIOD_DAYS);

        List<RunningRecordRepository.PopularCourseProjection> courses = runningRecordRepository
                .findPopularCourses(startedAt, endedAt, PageRequest.of(0, POPULAR_COURSE_LIMIT));

        List<PopularCoursesResponse.PopularCourseItem> rankedCourses = IntStream
                .range(0, courses.size())
                .mapToObj(index -> {
                    RunningRecordRepository.PopularCourseProjection course = courses.get(index);
                    return new PopularCoursesResponse.PopularCourseItem(
                            course.getCourseId(),
                            index + 1,
                            course.getCourseName(),
                            course.getDistanceKm(),
                            course.getDifficulty(),
                            course.getParticipantCount(),
                            course.getThumbnailImageUrl()
                    );
                })
                .toList();

        return new PopularCoursesResponse(rankedCourses);
    }
}
