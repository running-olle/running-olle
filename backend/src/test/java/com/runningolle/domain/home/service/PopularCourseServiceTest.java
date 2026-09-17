package com.runningolle.domain.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.home.dto.PopularCoursesResponse;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PopularCourseServiceTest {

    @Mock
    private RunningRecordRepository runningRecordRepository;

    @Test
    void returnsAtMostFivePopularCoursesWithSequentialRanksForTheLastThirtyDays() {
        RunningRecordRepository.PopularCourseProjection first = popularCourse(
                "A Course", 7, Difficulty.LOW
        );
        RunningRecordRepository.PopularCourseProjection second = popularCourse(
                "B Course", 7, Difficulty.MID
        );
        given(runningRecordRepository.findPopularCourses(
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(PageRequest.of(0, 5))
        )).willReturn(List.of(first, second));
        PopularCourseService service = new PopularCourseService(runningRecordRepository);
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);

        PopularCoursesResponse response = service.getPopularCourses();

        LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC);
        assertThat(response.courses()).extracting(PopularCoursesResponse.PopularCourseItem::rank)
                .containsExactly(1, 2);
        assertThat(response.courses()).extracting(PopularCoursesResponse.PopularCourseItem::courseName)
                .containsExactly("A Course", "B Course");

        ArgumentCaptor<LocalDateTime> startedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(runningRecordRepository).findPopularCourses(
                startedAtCaptor.capture(),
                endedAtCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(PageRequest.of(0, 5))
        );
        assertThat(endedAtCaptor.getValue()).isBetween(before, after);
        assertThat(startedAtCaptor.getValue()).isEqualTo(endedAtCaptor.getValue().minusDays(30));
    }

    @Test
    void returnsEmptyCoursesWhenThereAreNoRunsInTheLastThirtyDays() {
        given(runningRecordRepository.findPopularCourses(
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(PageRequest.of(0, 5))
        )).willReturn(List.of());
        PopularCourseService service = new PopularCourseService(runningRecordRepository);

        PopularCoursesResponse response = service.getPopularCourses();

        assertThat(response.courses()).isEmpty();
    }

    private RunningRecordRepository.PopularCourseProjection popularCourse(
            String name,
            long participantCount,
            Difficulty difficulty
    ) {
        RunningRecordRepository.PopularCourseProjection projection = mock(
                RunningRecordRepository.PopularCourseProjection.class
        );
        when(projection.getCourseId()).thenReturn(UUID.randomUUID());
        when(projection.getCourseName()).thenReturn(name);
        when(projection.getDistanceKm()).thenReturn(new BigDecimal("5.50"));
        when(projection.getDifficulty()).thenReturn(difficulty);
        when(projection.getParticipantCount()).thenReturn(participantCount);
        when(projection.getThumbnailImageUrl()).thenReturn(null);
        return projection;
    }
}
