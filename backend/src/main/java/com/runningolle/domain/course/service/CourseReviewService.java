package com.runningolle.domain.course.service;

import com.runningolle.domain.course.dto.CourseReviewCreateRequest;
import com.runningolle.domain.course.dto.CourseReviewListResponse;
import com.runningolle.domain.course.dto.CourseReviewResponse;
import com.runningolle.domain.course.dto.CourseReviewUpdateRequest;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseReview;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseReviewRepository;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewService {

    private static final String KAKAO_PREFIX = "kakao:";

    private final CourseReviewRepository courseReviewRepository;
    private final CourseRepository courseRepository;
    private final RunningRecordRepository runningRecordRepository;
    private final UserRepository userRepository;

    public CourseReviewListResponse getCourseReviews(UUID courseId, String principalName) {
        User currentUser = resolveCurrentUser(principalName);
        Course course = getActiveCourse(courseId);
        List<CourseReviewResponse> reviews = courseReviewRepository.findAllByCourseIdOrderByCreatedAtDesc(course.getId())
                .stream()
                .map(review -> toResponse(review, currentUser))
                .toList();

        return new CourseReviewListResponse(
                normalizeRating(course.getRatingAvg()),
                courseReviewRepository.countByCourseId(course.getId()),
                reviews
        );
    }

    @Transactional
    public CourseReviewResponse createReview(UUID courseId, CourseReviewCreateRequest request, String principalName) {
        User currentUser = resolveCurrentUser(principalName);
        Course course = getActiveCourse(courseId);
        RunningRecord runningRecord = getRunningRecord(request.runningRecordId());

        validateRatingStep(request.rating());
        validateRunningRecordOwnership(currentUser, runningRecord);
        validateRunningRecordCourse(course, runningRecord);
        validateDuplicateReview(runningRecord.getId());

        CourseReview review = CourseReview.create(
                currentUser,
                course,
                runningRecord,
                request.rating(),
                request.content()
        );

        CourseReview savedReview = courseReviewRepository.save(review);
        refreshCourseRating(course);
        return toResponse(savedReview, currentUser);
    }

    @Transactional
    public CourseReviewResponse updateReview(
            UUID courseId,
            UUID reviewId,
            CourseReviewUpdateRequest request,
            String principalName
    ) {
        User currentUser = resolveCurrentUser(principalName);
        CourseReview review = getOwnedReview(courseId, reviewId, currentUser);

        validateRatingStep(request.rating());
        review.update(request.rating(), request.content());
        refreshCourseRating(review.getCourse());
        return toResponse(review, currentUser);
    }

    @Transactional
    public void deleteReview(UUID courseId, UUID reviewId, String principalName) {
        User currentUser = resolveCurrentUser(principalName);
        CourseReview review = getOwnedReview(courseId, reviewId, currentUser);
        Course course = review.getCourse();

        courseReviewRepository.delete(review);
        refreshCourseRating(course);
    }

    private User resolveCurrentUser(String principalName) {
        if (principalName == null || !principalName.startsWith(KAKAO_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized user.");
        }

        String kakaoId = principalName.substring(KAKAO_PREFIX.length());
        return userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
    }

    private Course getActiveCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));

        if (Boolean.TRUE.equals(course.getIsDeleted())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found.");
        }
        return course;
    }

    private RunningRecord getRunningRecord(UUID runningRecordId) {
        return runningRecordRepository.findById(runningRecordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Running record not found."));
    }

    private void validateRunningRecordOwnership(User currentUser, RunningRecord runningRecord) {
        if (!runningRecord.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only review your own running record.");
        }
    }

    private void validateRunningRecordCourse(Course course, RunningRecord runningRecord) {
        if (runningRecord.getCourse() == null || !runningRecord.getCourse().getId().equals(course.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Running record does not belong to this course.");
        }
    }

    private void validateDuplicateReview(UUID runningRecordId) {
        if (courseReviewRepository.existsByRunningRecordId(runningRecordId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review already exists for this running record.");
        }
    }

    private void validateRatingStep(BigDecimal rating) {
        if (rating == null) {
            return;
        }

        BigDecimal doubled = rating.multiply(BigDecimal.valueOf(2)).stripTrailingZeros();

        if (doubled.scale() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be in 0.5 increments.");
        }
    }

    private CourseReview getOwnedReview(UUID courseId, UUID reviewId, User currentUser) {
        CourseReview review = courseReviewRepository.findByIdAndCourseId(reviewId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found."));

        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own review.");
        }
        return review;
    }

    private void refreshCourseRating(Course course) {
        Double averageRating = courseReviewRepository.findAverageRatingByCourseId(course.getId());
        BigDecimal ratingAvg = averageRating == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP);
        course.updateRatingAvg(ratingAvg);
    }

    private BigDecimal normalizeRating(BigDecimal ratingAvg) {
        return ratingAvg == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : ratingAvg;
    }

    private CourseReviewResponse toResponse(CourseReview review, User currentUser) {
        return new CourseReviewResponse(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getNickname(),
                review.getUser().getProfileImageUrl(),
                review.getRunningRecord().getId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getUser().getId().equals(currentUser.getId())
        );
    }
}
