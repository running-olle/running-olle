package com.runningolle.domain.course.service;

import com.runningolle.domain.course.dto.CourseReviewRequest;
import com.runningolle.domain.course.dto.CourseReviewResponse;
import com.runningolle.domain.course.dto.CourseReviewUpdateRequest;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseReview;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseReviewRepository;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CourseReviewService {

    private final CourseRepository courseRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final RunningRecordRepository runningRecordRepository;

    @Transactional(readOnly = true)
    public List<CourseReviewResponse> getReviews(UUID userId, UUID courseId) {
        getVisibleCourse(courseId, userId);
        return courseReviewRepository.findAllByCourse_IdOrderByCreatedAtDesc(courseId).stream()
                .map(review -> CourseReviewResponse.from(review, userId))
                .toList();
    }

    @Transactional
    public CourseReviewResponse createReview(UUID userId, UUID courseId, CourseReviewRequest request) {
        Course course = getVisibleCourse(courseId, userId);
        RunningRecord runningRecord = runningRecordRepository.findByIdAndUserId(request.runningRecordId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "러닝 기록을 찾을 수 없습니다."));

        if (runningRecord.getCourse() == null || !runningRecord.getCourse().getId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 코스를 달린 기록으로만 리뷰를 남길 수 있습니다.");
        }
        if (courseReviewRepository.existsByRunningRecord_Id(runningRecord.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이 러닝 기록에는 이미 리뷰를 작성했습니다.");
        }

        CourseReview review = courseReviewRepository.saveAndFlush(CourseReview.create(
                runningRecord.getUser(),
                course,
                runningRecord,
                request.rating(),
                normalizeContent(request.content())
        ));
        refreshRatingAverage(course);
        return CourseReviewResponse.from(review, userId);
    }

    @Transactional
    public CourseReviewResponse updateReview(
            UUID userId,
            UUID courseId,
            UUID reviewId,
            CourseReviewUpdateRequest request
    ) {
        Course course = getVisibleCourse(courseId, userId);
        CourseReview review = getReview(courseId, reviewId);
        requireAuthor(review, userId);
        review.update(request.rating(), normalizeContent(request.content()));
        courseReviewRepository.flush();
        refreshRatingAverage(course);
        return CourseReviewResponse.from(review, userId);
    }

    @Transactional
    public void deleteReview(UUID userId, UUID courseId, UUID reviewId) {
        Course course = getVisibleCourse(courseId, userId);
        CourseReview review = getReview(courseId, reviewId);
        requireAuthor(review, userId);
        courseReviewRepository.delete(review);
        courseReviewRepository.flush();
        refreshRatingAverage(course);
    }

    private Course getVisibleCourse(UUID courseId, UUID userId) {
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."));
        if (!Boolean.TRUE.equals(course.getIsPublic()) && !course.getCreator().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다.");
        }
        return course;
    }

    private CourseReview getReview(UUID courseId, UUID reviewId) {
        return courseReviewRepository.findByIdAndCourse_Id(reviewId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."));
    }

    private void requireAuthor(CourseReview review, UUID userId) {
        if (!review.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 작성한 리뷰만 변경할 수 있습니다.");
        }
    }

    private void refreshRatingAverage(Course course) {
        Double average = courseReviewRepository.findAverageRatingByCourseId(course.getId());
        course.updateRatingAverage(average == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
    }

    private String normalizeContent(String content) {
        return StringUtils.hasText(content) ? content.trim() : null;
    }
}
