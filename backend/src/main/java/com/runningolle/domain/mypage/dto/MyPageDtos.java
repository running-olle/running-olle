package com.runningolle.domain.mypage.dto;

import com.runningolle.domain.course.dto.CourseWaypointResponse;
import com.runningolle.domain.course.dto.RouteCoordinateResponse;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.running.enums.RunningMode;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    public record Visit(UUID id, UUID waypointId, UUID courseId, String courseName, String name,
                        String description, String imageUrl, LocalDateTime visitedAt, int orderIndex,
                        double latitude, double longitude) {}
    public record RunDetail(UUID id, UUID courseId, String courseName, String courseDescription,
                            CourseType courseType, Difficulty courseDifficulty, String thumbnailImageUrl,
                            RunningMode runningMode, BigDecimal distanceKm, int durationSeconds,
                            BigDecimal averagePace, BigDecimal calories, BigDecimal elevationGainM,
                            LocalDateTime startedAt, LocalDateTime endedAt,
                            List<RouteCoordinateResponse> recordedRouteCoordinates,
                            List<RouteCoordinateResponse> plannedRouteCoordinates,
                            List<CourseWaypointResponse> courseWaypoints) {}
    public record Bookmark(UUID bookmarkId, UUID courseId, String name, CourseType courseType,
                           BigDecimal distanceKm, Difficulty difficulty, String thumbnailImageUrl, boolean mine) {}
    public record TripResponse(UUID id, String name, String region, LocalDate startDate, LocalDate endDate,
                               String thumbnailImageUrl, long completedCourses, BigDecimal totalDistanceKm,
                               long visitedPlaces, long totalDurationSeconds) {}
    public record CreateTripRequest(String name, String region, LocalDate startDate, LocalDate endDate, String thumbnailImageUrl) {}
    public record UpdateProfileRequest(@NotBlank @Size(min = 2, max = 100) String nickname,
                                       @Size(max = 4_200_000) String profileImageUrl,
                                       @Size(max = 300) String bio,
                                       @Size(max = 3) List<String> userTypes,
                                       PreferredDistance preferredDistance, PreferredDifficulty preferredDifficulty) {}
    public record NotificationSettings(boolean recommendedCourse, boolean weather, boolean savedCourseUpdate,
                                       boolean meetupInvite, boolean commentLike, boolean tierChange, boolean eventChallenge) {}
}
