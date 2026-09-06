package com.runningolle.domain.community.service;

import com.runningolle.domain.community.dto.FeedCommentCreateRequest;
import com.runningolle.domain.community.dto.FeedCommentResponse;
import com.runningolle.domain.community.dto.FeedLikeToggleResponse;
import com.runningolle.domain.community.dto.FeedPostCreateRequest;
import com.runningolle.domain.community.dto.FeedPostResponse;
import com.runningolle.domain.community.dto.FeedSelectionOptionResponse;
import com.runningolle.domain.community.dto.FeedPostUpdateRequest;
import com.runningolle.domain.community.entity.FeedComment;
import com.runningolle.domain.community.entity.FeedLike;
import com.runningolle.domain.community.entity.FeedPost;
import com.runningolle.domain.community.entity.FeedPostImage;
import com.runningolle.domain.community.enums.Visibility;
import com.runningolle.domain.community.repository.FeedCommentRepository;
import com.runningolle.domain.community.repository.FeedLikeRepository;
import com.runningolle.domain.community.repository.FeedPostImageRepository;
import com.runningolle.domain.community.repository.FeedPostRepository;
import com.runningolle.domain.community.storage.FileStorageService;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.notification.enums.NotificationType;
import com.runningolle.domain.notification.service.NotificationService;
import com.runningolle.domain.running.entity.RunningRecord;
import com.runningolle.domain.running.repository.RunningRecordRepository;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.repository.UserRepository;
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
public class FeedService {

    private static final String FEED_REGION_JEJU = "제주";

    private final FeedPostRepository feedPostRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedPostImageRepository feedPostImageRepository;
    private final UserRepository userRepository;
    private final RunningRecordRepository runningRecordRepository;
    private final CourseRepository courseRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<FeedPostResponse> getFeed(UUID userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return feedPostRepository.findRecentVisibleByRegion(FEED_REGION_JEJU, since).stream()
                .filter(post -> post.getVisibility() == Visibility.PUBLIC || post.getUser().getId().equals(userId))
                .map(post -> toResponse(post, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedSelectionOptionResponse> getMyRunningRecordOptions(UUID userId) {
        return runningRecordRepository.findTop10ByUserIdOrderByStartedAtDesc(userId).stream()
                .map(record -> new FeedSelectionOptionResponse(
                        record.getId(),
                        record.getCourse() != null ? record.getCourse().getName() : "즉시 달리기 기록",
                        record.getCourse() != null ? record.getCourse().getCourseType() : null,
                        record.getTotalDistanceKm().doubleValue(),
                        record.getTotalDurationSeconds()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeedSelectionOptionResponse> getCourseOptions() {
        return courseRepository.findTop10ByIsDeletedFalseAndIsPublicTrueOrderByCreatedAtDesc().stream()
                .map(course -> new FeedSelectionOptionResponse(
                        course.getId(),
                        course.getName(),
                        course.getCourseType(),
                        course.getDistanceKm().doubleValue(),
                        course.getEstimatedDurationMinutes() * 60
                ))
                .toList();
    }

    @Transactional
    public FeedPostResponse createPost(UUID userId, FeedPostCreateRequest request) {
        User user = getUser(userId);
        RunningRecord runningRecord = getRunningRecord(request.runningRecordId(), userId);
        Course course = getCourse(request.courseId(), userId);

        FeedPost feedPost = feedPostRepository.save(FeedPost.create(
                user,
                runningRecord,
                course,
                request.content().trim(),
                request.visibility(),
                request.region().trim(),
                request.photoTagged()
        ));

        List<String> imageUrls = request.imageUrls() == null ? List.of() : request.imageUrls();
        for (int index = 0; index < imageUrls.size(); index++) {
            feedPostImageRepository.save(FeedPostImage.create(feedPost, imageUrls.get(index).trim(), index));
        }

        return toResponse(feedPost, userId);
    }

    @Transactional(readOnly = true)
    public FeedPostResponse getPost(UUID userId, UUID feedPostId) {
        return toResponse(getFeedPost(feedPostId, userId), userId);
    }

    @Transactional
    public FeedPostResponse updatePost(UUID userId, UUID feedPostId, FeedPostUpdateRequest request) {
        FeedPost feedPost = feedPostRepository.findByIdAndIsDeletedFalse(feedPostId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (!feedPost.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 게시글만 수정할 수 있습니다.");
        }

        Course course = getCourse(request.courseId(), userId);
        feedPost.update(
                course,
                request.content().trim(),
                request.visibility(),
                request.region().trim(),
                request.photoTagged()
        );

        feedPostImageRepository.findByFeedPostIdOrderByOrderIndexAsc(feedPostId)
                .forEach(image -> fileStorageService.deleteByUrl(image.getImageUrl()));
        feedPostImageRepository.deleteByFeedPostId(feedPostId);
        List<String> imageUrls = request.imageUrls() == null ? List.of() : request.imageUrls();
        for (int index = 0; index < imageUrls.size(); index++) {
            feedPostImageRepository.save(FeedPostImage.create(feedPost, imageUrls.get(index).trim(), index));
        }

        return toResponse(feedPost, userId);
    }

    @Transactional
    public FeedLikeToggleResponse toggleLike(UUID userId, UUID feedPostId) {
        FeedPost feedPost = getFeedPost(feedPostId, userId);

        return feedLikeRepository.findByFeedPostIdAndUserId(feedPostId, userId)
                .map(existing -> {
                    feedLikeRepository.delete(existing);
                    return new FeedLikeToggleResponse(false, feedLikeRepository.countByFeedPostId(feedPostId));
                })
                .orElseGet(() -> {
                    User actor = getUser(userId);
                    feedLikeRepository.save(FeedLike.create(actor, feedPost));
                    if (!feedPost.getUser().getId().equals(userId)) {
                        notificationService.createSocial(
                                feedPost.getUser(),
                                NotificationType.FEED_LIKE,
                                "게시글에 새 좋아요가 있어요",
                                actor.getNickname() + "님이 회원님의 게시글을 좋아합니다.",
                                "FEED_LIKE:" + userId + ":" + feedPostId
                        );
                    }
                    return new FeedLikeToggleResponse(true, feedLikeRepository.countByFeedPostId(feedPostId));
                });
    }

    @Transactional
    public FeedCommentResponse addComment(UUID userId, UUID feedPostId, FeedCommentCreateRequest request) {
        FeedPost feedPost = getFeedPost(feedPostId, userId);
        User actor = getUser(userId);
        FeedComment feedComment = feedCommentRepository.save(
                FeedComment.create(feedPost, actor, request.content().trim())
        );
        if (!feedPost.getUser().getId().equals(userId)) {
            notificationService.createSocial(
                    feedPost.getUser(),
                    NotificationType.FEED_COMMENT,
                    "게시글에 새 댓글이 달렸어요",
                    actor.getNickname() + "님: " + summarize(request.content()),
                    "FEED_COMMENT:" + feedComment.getId()
            );
        }
        return toCommentResponse(feedComment, userId);
    }

    @Transactional
    public void deleteComment(UUID userId, UUID commentId) {
        FeedComment comment = feedCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        if (!comment.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 댓글만 삭제할 수 있습니다.");
        }
        comment.delete();
    }

    @Transactional
    public void deletePost(UUID userId, UUID feedPostId) {
        FeedPost feedPost = feedPostRepository.findByIdAndIsDeletedFalse(feedPostId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (!feedPost.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 게시글만 삭제할 수 있습니다.");
        }
        feedPostImageRepository.findByFeedPostIdOrderByOrderIndexAsc(feedPostId)
                .forEach(image -> fileStorageService.deleteByUrl(image.getImageUrl()));
        feedPost.delete();
    }

    private FeedPostResponse toResponse(FeedPost feedPost, UUID userId) {
        List<FeedCommentResponse> comments = feedCommentRepository
                .findByFeedPostIdAndIsDeletedFalseOrderByCreatedAtAsc(feedPost.getId())
                .stream()
                .map(comment -> toCommentResponse(comment, userId))
                .toList();

        List<String> imageUrls = feedPostImageRepository.findByFeedPostIdOrderByOrderIndexAsc(feedPost.getId())
                .stream()
                .map(FeedPostImage::getImageUrl)
                .toList();

        FeedPostResponse.FeedRunningRecordSummary runningRecord = feedPost.getRunningRecord() == null
                ? null
                : new FeedPostResponse.FeedRunningRecordSummary(
                        feedPost.getRunningRecord().getId(),
                        feedPost.getRunningRecord().getTotalDistanceKm().doubleValue(),
                        feedPost.getRunningRecord().getTotalDurationSeconds()
                );

        FeedPostResponse.FeedCourseSummary course = feedPost.getCourse() == null
                ? null
                : new FeedPostResponse.FeedCourseSummary(
                        feedPost.getCourse().getId(),
                        feedPost.getCourse().getName(),
                        feedPost.getCourse().getCourseType()
                );

        return new FeedPostResponse(
                feedPost.getId(),
                feedPost.getUser().getId(),
                feedPost.getUser().getId().equals(userId),
                feedPost.getUser().getNickname(),
                feedPost.getRegion(),
                feedPost.getContent(),
                feedPost.getVisibility(),
                Boolean.TRUE.equals(feedPost.getIsPhotoTagged()),
                feedLikeRepository.existsByFeedPostIdAndUserId(feedPost.getId(), userId),
                feedLikeRepository.countByFeedPostId(feedPost.getId()),
                feedCommentRepository.countByFeedPostIdAndIsDeletedFalse(feedPost.getId()),
                feedPost.getCreatedAt(),
                runningRecord,
                course,
                imageUrls,
                comments
        );
    }

    private FeedCommentResponse toCommentResponse(FeedComment comment, UUID userId) {
        return new FeedCommentResponse(
                comment.getId(),
                comment.getUser().getId(),
                comment.getUser().getNickname(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUser().getId().equals(userId)
        );
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private FeedPost getFeedPost(UUID feedPostId, UUID userId) {
        FeedPost feedPost = feedPostRepository.findByIdAndIsDeletedFalse(feedPostId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
        if (feedPost.getVisibility() == Visibility.PRIVATE && !feedPost.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비공개 게시글입니다.");
        }
        return feedPost;
    }

    private RunningRecord getRunningRecord(UUID runningRecordId, UUID userId) {
        if (runningRecordId == null) {
            return null;
        }
        return runningRecordRepository.findByIdAndUserId(runningRecordId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "연결할 러닝 기록이 올바르지 않습니다."));
    }

    private Course getCourse(UUID courseId, UUID userId) {
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

    private String summarize(String content) {
        String normalized = content.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80) + "…";
    }
}
