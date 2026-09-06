package com.runningolle.domain.mypage.dto;

import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class MyPageDtos {
    private MyPageDtos() {}

    public record Profile(String nickname, String profileImageUrl, String bio, List<String> userTypes,
                          PreferredDistance preferredDistance, PreferredDifficulty preferredDifficulty,
                          LocalDateTime createdAt, String accountStatus) {}
    public record Dashboard(Profile profile, BigDecimal totalDistanceKm, long completionCount, long uniqueCourseCount) {}
    public record Run(UUID id, UUID courseId, String courseName, CourseType courseType, String thumbnailImageUrl,
                      BigDecimal distanceKm, int durationSeconds, BigDecimal averagePace, LocalDateTime startedAt) {}
    public record Bookmark(UUID bookmarkId, UUID courseId, String name, CourseType courseType,
                           BigDecimal distanceKm, Difficulty difficulty, String thumbnailImageUrl, boolean mine) {}
    public record TripResponse(UUID id, String name, String region, LocalDate startDate, LocalDate endDate,
                               String thumbnailImageUrl, long completedCourses, BigDecimal totalDistanceKm,
                               long visitedPlaces, long totalDurationSeconds) {}
    public record CreateTripRequest(String name, String region, LocalDate startDate, LocalDate endDate, String thumbnailImageUrl) {}
    public record UpdateProfileRequest(String nickname, String profileImageUrl, String bio, List<String> userTypes,
                                       PreferredDistance preferredDistance, PreferredDifficulty preferredDifficulty,
                                       List<UUID> themeIds) {}
    public record NotificationSettings(boolean recommendedCourse, boolean weather, boolean savedCourseUpdate,
                                       boolean meetupInvite, boolean commentLike, boolean tierChange, boolean eventChallenge) {}
}
