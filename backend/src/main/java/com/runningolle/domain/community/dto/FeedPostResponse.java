package com.runningolle.domain.community.dto;

import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.community.enums.Visibility;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FeedPostResponse(
        UUID id,
        UUID userId,
        boolean mine,
        String nickname,
        String profileImageUrl,
        String region,
        String content,
        Visibility visibility,
        boolean photoTagged,
        boolean likedByMe,
        long likeCount,
        long commentCount,
        LocalDateTime createdAt,
        FeedRunningRecordSummary runningRecord,
        FeedCourseSummary course,
        List<String> imageUrls,
        List<FeedCommentResponse> comments
) {
    public record FeedRunningRecordSummary(
            UUID id,
            double distanceKm,
            int durationSeconds
    ) {
    }

    public record FeedCourseSummary(
            UUID id,
            String name,
            CourseType courseType
    ) {
    }
}
