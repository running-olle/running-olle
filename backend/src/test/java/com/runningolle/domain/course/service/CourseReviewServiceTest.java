package com.runningolle.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.runningolle.domain.course.dto.CourseReviewRequest;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseReview;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseReviewRepository;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import com.runningolle.domain.user.entity.User;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CourseReviewServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseReviewRepository courseReviewRepository;
    @Mock
    private RunningRecordRepository runningRecordRepository;
    @Mock
    private Course course;
    @Mock
    private RunningRecord runningRecord;
    @Mock
    private User user;

    private CourseReviewService service;
    private UUID userId;
    private UUID courseId;
    private UUID recordId;

    @BeforeEach
    void setUp() {
        service = new CourseReviewService(courseRepository, courseReviewRepository, runningRecordRepository);
        userId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        recordId = UUID.randomUUID();
    }

    @Test
    void createsReviewForOwnedRunningRecordAndRefreshesAverage() {
        givenVisibleCourse();
        given(course.getId()).willReturn(courseId);
        given(runningRecordRepository.findByIdAndUserId(recordId, userId)).willReturn(Optional.of(runningRecord));
        given(runningRecord.getId()).willReturn(recordId);
        given(runningRecord.getCourse()).willReturn(course);
        given(runningRecord.getUser()).willReturn(user);
        given(user.getId()).willReturn(userId);
        given(user.getNickname()).willReturn("올레러너");
        given(courseReviewRepository.existsByRunningRecord_Id(recordId)).willReturn(false);
        given(courseReviewRepository.saveAndFlush(any(CourseReview.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(courseReviewRepository.findAverageRatingByCourseId(courseId)).willReturn(4.5);

        var response = service.createReview(
                userId,
                courseId,
                new CourseReviewRequest(recordId, 5, "  바다 풍경이 좋았어요.  ")
        );

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.content()).isEqualTo("바다 풍경이 좋았어요.");
        assertThat(response.authoredByMe()).isTrue();
        verify(course).updateRatingAverage(new BigDecimal("4.50"));
    }

    @Test
    void rejectsReviewWhenRunningRecordBelongsToAnotherCourse() {
        givenVisibleCourse();
        Course anotherCourse = org.mockito.Mockito.mock(Course.class);
        given(anotherCourse.getId()).willReturn(UUID.randomUUID());
        given(runningRecordRepository.findByIdAndUserId(recordId, userId)).willReturn(Optional.of(runningRecord));
        given(runningRecord.getCourse()).willReturn(anotherCourse);

        assertThatThrownBy(() -> service.createReview(
                userId,
                courseId,
                new CourseReviewRequest(recordId, 4, null)
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("해당 코스를 달린 기록");

        verify(courseReviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsSecondReviewForSameRunningRecord() {
        givenVisibleCourse();
        given(course.getId()).willReturn(courseId);
        given(runningRecordRepository.findByIdAndUserId(recordId, userId)).willReturn(Optional.of(runningRecord));
        given(runningRecord.getId()).willReturn(recordId);
        given(runningRecord.getCourse()).willReturn(course);
        given(courseReviewRepository.existsByRunningRecord_Id(recordId)).willReturn(true);

        assertThatThrownBy(() -> service.createReview(
                userId,
                courseId,
                new CourseReviewRequest(recordId, 4, null)
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 리뷰");

        verify(courseReviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void deletesOwnReviewAndResetsAverageWhenItWasLastReview() {
        givenVisibleCourse();
        given(course.getId()).willReturn(courseId);
        UUID reviewId = UUID.randomUUID();
        CourseReview review = org.mockito.Mockito.mock(CourseReview.class);
        given(review.getUser()).willReturn(user);
        given(user.getId()).willReturn(userId);
        given(courseReviewRepository.findByIdAndCourse_Id(reviewId, courseId)).willReturn(Optional.of(review));
        given(courseReviewRepository.findAverageRatingByCourseId(courseId)).willReturn(null);

        service.deleteReview(userId, courseId, reviewId);

        verify(courseReviewRepository).delete(review);
        verify(course).updateRatingAverage(new BigDecimal("0.00"));
    }

    @Test
    void preventsDeletingAnotherUsersReview() {
        givenVisibleCourse();
        UUID reviewId = UUID.randomUUID();
        CourseReview review = org.mockito.Mockito.mock(CourseReview.class);
        given(review.getUser()).willReturn(user);
        given(user.getId()).willReturn(UUID.randomUUID());
        given(courseReviewRepository.findByIdAndCourse_Id(reviewId, courseId)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> service.deleteReview(userId, courseId, reviewId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("내가 작성한 리뷰만");

        verify(courseReviewRepository, never()).delete(any());
    }

    private void givenVisibleCourse() {
        given(courseRepository.findByIdAndIsDeletedFalse(courseId)).willReturn(Optional.of(course));
        given(course.getIsPublic()).willReturn(true);
    }
}
