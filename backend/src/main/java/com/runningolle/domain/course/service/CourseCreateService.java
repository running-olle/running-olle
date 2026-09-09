package com.runningolle.domain.course.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.domain.course.dto.CourseCreateRequest;
import com.runningolle.domain.course.dto.CourseCreateRequest.WaypointRequest;
import com.runningolle.domain.course.dto.CourseCreateResponse;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseTag;
import com.runningolle.domain.course.entity.CourseTagMap;
import com.runningolle.domain.course.entity.CourseTheme;
import com.runningolle.domain.course.entity.CourseWaypoint;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseTagMapRepository;
import com.runningolle.domain.course.repository.CourseTagRepository;
import com.runningolle.domain.course.repository.CourseThemeRepository;
import com.runningolle.domain.course.repository.CourseWaypointRepository;
import com.runningolle.domain.home.service.CourseRecommendationDocumentService;
import com.runningolle.domain.home.service.CourseRecommendationEmbeddingService;
import com.runningolle.domain.routing.client.OpenRouteServiceClient;
import com.runningolle.domain.routing.client.OpenRouteServiceClient.OrsRouteResult;
import com.runningolle.domain.routing.client.OpenRouteServiceClient.SurfaceBreakdown;
import com.runningolle.domain.routing.client.OpenRouteServiceClient.Waypoint;
import com.runningolle.domain.tourism.client.TourApiClient;
import com.runningolle.domain.tourism.client.TourApiClient.TourDetail;
import com.runningolle.domain.user.entity.Theme;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.enums.AccountStatus;
import com.runningolle.domain.user.repository.ThemeRepository;
import com.runningolle.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseCreateService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final CourseRepository courseRepository;
    private final CourseWaypointRepository courseWaypointRepository;
    private final CourseThemeRepository courseThemeRepository;
    private final CourseTagMapRepository courseTagMapRepository;
    private final CourseTagRepository courseTagRepository;
    private final ThemeRepository themeRepository;
    private final UserRepository userRepository;
    private final OpenRouteServiceClient openRouteServiceClient;
    private final TourApiClient tourApiClient;
    private final ObjectMapper objectMapper;
    private final CourseRecommendationDocumentService courseRecommendationDocumentService;
    private final CourseRecommendationEmbeddingService courseRecommendationEmbeddingService;

    @Transactional
    public CourseCreateResponse createCourse(UUID creatorId, CourseCreateRequest request) {
        User creator = getActiveUser(creatorId);
        List<WaypointRequest> waypoints = sortedWaypoints(request);
        List<Theme> themes = findThemes(request.themeIds());
        List<CourseTag> courseTags = findCourseTags(request.tagIds());
        OrsRouteResult routeResult = openRouteServiceClient.calculateFootWalkingRoute(toRoutingWaypoints(waypoints));
        List<ResolvedWaypoint> resolvedWaypoints = resolveWaypointTourData(waypoints);

        Course course = courseRepository.save(Course.create(
                creator,
                request.name().trim(),
                trimToNull(request.description()),
                request.courseType(),
                decimal(routeResult.distanceKm()),
                routeResult.estimatedDurationMinutes(),
                decimal(routeResult.elevationGainM()),
                CourseDifficultyCalculator.suggest(routeResult.distanceKm(), routeResult.elevationGainM()),
                surfacePct(routeResult.surface(), SurfaceKind.ASPHALT),
                surfacePct(routeResult.surface(), SurfaceKind.DIRT),
                surfacePct(routeResult.surface(), SurfaceKind.STAIRS),
                routeResult.routeLineString(),
                point(waypoints.get(0).lng(), waypoints.get(0).lat()),
                thumbnailImageUrl(resolvedWaypoints),
                request.isPublic() == null || request.isPublic()
        ));

        courseWaypointRepository.saveAll(toCourseWaypoints(course, resolvedWaypoints, routeResult.segmentDistanceKm()));
        courseThemeRepository.saveAll(toCourseThemes(course, themes));
        courseTagMapRepository.saveAll(toCourseTagMaps(course, courseTags));
        courseRecommendationDocumentService.syncCourseDescriptionDocument(course.getId());
        courseRecommendationEmbeddingService.syncCourseRecommendationEmbeddings(course.getId());

        return new CourseCreateResponse(course.getId());
    }

    private User getActiveUser(UUID creatorId) {
        User user = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "활성 계정이 아닙니다.");
        }
        return user;
    }

    private static List<WaypointRequest> sortedWaypoints(CourseCreateRequest request) {
        if (request == null || request.waypoints() == null || request.waypoints().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "코스 저장에는 최소 2개 이상의 경유지가 필요합니다.");
        }

        Set<Integer> orderIndexes = new HashSet<>();
        for (WaypointRequest waypoint : request.waypoints()) {
            if (waypoint.orderIndex() == null || !orderIndexes.add(waypoint.orderIndex())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "경유지 순서가 올바르지 않습니다.");
            }
        }

        return request.waypoints().stream()
                .sorted(Comparator.comparing(WaypointRequest::orderIndex))
                .toList();
    }

    private static List<Waypoint> toRoutingWaypoints(List<WaypointRequest> waypoints) {
        return waypoints.stream()
                .map(waypoint -> new Waypoint(waypoint.name().trim(), waypoint.lat(), waypoint.lng()))
                .toList();
    }

    private List<ResolvedWaypoint> resolveWaypointTourData(List<WaypointRequest> waypoints) {
        LocalDateTime now = LocalDateTime.now();
        return waypoints.stream()
                .map(waypoint -> resolveWaypointTourData(waypoint, now))
                .toList();
    }

    private ResolvedWaypoint resolveWaypointTourData(WaypointRequest waypoint, LocalDateTime syncedAt) {
        String tourContentId = trimToNull(waypoint.tourContentId());
        String tourContentTypeId = trimToNull(waypoint.tourContentTypeId());
        JsonNode tourData = nullIfEmpty(waypoint.tourDataRaw());
        String firstImageUrl = trimToNull(waypoint.firstImageUrl());
        String thumbnailImageUrl = trimToNull(waypoint.thumbnailImageUrl());
        LocalDateTime tourSyncedAt = tourData == null ? null : syncedAt;

        if (StringUtils.hasText(tourContentId) && tourData == null) {
            try {
                Optional<TourDetail> detail = tourApiClient.getDetail(tourContentId, tourContentTypeId);
                if (detail.isPresent()) {
                    TourDetail tourDetail = detail.get();
                    tourData = objectMapper.valueToTree(tourDetail.raw());
                    tourContentTypeId = firstNonBlank(tourDetail.contentTypeId(), tourContentTypeId);
                    firstImageUrl = firstNonBlank(tourDetail.firstImageUrl(), firstImageUrl);
                    tourSyncedAt = syncedAt;
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "Failed to enrich waypoint TourAPI data. tourContentId={}, waypointName={}",
                        tourContentId,
                        waypoint.name(),
                        exception
                );
            }
        }

        String resolvedFirstImageUrl = firstNonBlank(firstImageUrl, firstImageUrl(tourData));

        return new ResolvedWaypoint(
                waypoint,
                tourContentId,
                tourContentTypeId,
                tourData,
                tourSyncedAt,
                resolvedFirstImageUrl,
                firstNonBlank(thumbnailImageUrl, resolvedFirstImageUrl)
        );
    }

    private List<CourseWaypoint> toCourseWaypoints(
            Course course,
            List<ResolvedWaypoint> waypoints,
            List<Double> segmentDistanceKm
    ) {
        List<BigDecimal> distanceFromStartKm = distanceFromStartKm(waypoints, segmentDistanceKm);
        List<CourseWaypoint> courseWaypoints = new ArrayList<>();
        for (int index = 0; index < waypoints.size(); index++) {
            ResolvedWaypoint waypoint = waypoints.get(index);
            courseWaypoints.add(CourseWaypoint.create(
                    course,
                    waypoint.request().name().trim(),
                    trimToNull(waypoint.request().kakaoPlaceId()),
                    point(waypoint.request().lng(), waypoint.request().lat()),
                    waypoint.request().orderIndex(),
                    distanceFromStartKm.get(index),
                    trimToNull(waypoint.request().description()),
                    waypoint.tourContentId(),
                    waypoint.tourContentTypeId(),
                    waypoint.tourData(),
                    waypoint.tourSyncedAt()
            ));
        }
        return courseWaypoints;
    }

    private List<Theme> findThemes(List<UUID> themeIds) {
        List<UUID> ids = distinctIds(themeIds);
        if (ids.isEmpty()) {
            return List.of();
        }

        List<Theme> themes = themeRepository.findAllById(ids);
        if (themes.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 테마가 포함되어 있습니다.");
        }
        return themes;
    }

    private static List<CourseTheme> toCourseThemes(Course course, List<Theme> themes) {
        return themes.stream()
                .map(theme -> CourseTheme.of(course, theme))
                .toList();
    }

    private List<CourseTag> findCourseTags(List<UUID> tagIds) {
        List<UUID> ids = distinctIds(tagIds);
        if (ids.isEmpty()) {
            return List.of();
        }

        List<CourseTag> courseTags = courseTagRepository.findAllById(ids);
        if (courseTags.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 코스 태그가 포함되어 있습니다.");
        }
        return courseTags;
    }

    private static List<CourseTagMap> toCourseTagMaps(Course course, List<CourseTag> courseTags) {
        return courseTags.stream()
                .map(courseTag -> CourseTagMap.of(course, courseTag))
                .toList();
    }

    private static List<UUID> distinctIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        for (UUID id : ids) {
            if (id == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "참조 ID가 올바르지 않습니다.");
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private static List<BigDecimal> distanceFromStartKm(
            List<ResolvedWaypoint> waypoints,
            List<Double> segmentDistanceKm
    ) {
        List<BigDecimal> distances = new ArrayList<>();
        double cumulativeDistanceKm = 0;
        distances.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        for (int index = 1; index < waypoints.size(); index++) {
            if (segmentDistanceKm != null && segmentDistanceKm.size() == waypoints.size() - 1) {
                cumulativeDistanceKm += segmentDistanceKm.get(index - 1);
            } else {
                WaypointRequest previous = waypoints.get(index - 1).request();
                WaypointRequest current = waypoints.get(index).request();
                cumulativeDistanceKm += distanceMeters(previous.lat(), previous.lng(), current.lat(), current.lng()) / 1000.0;
            }
            distances.add(decimal(cumulativeDistanceKm));
        }

        return distances;
    }

    private static String thumbnailImageUrl(List<ResolvedWaypoint> waypoints) {
        return waypoints.stream()
                .map(waypoint -> firstNonBlank(waypoint.thumbnailImageUrl(), waypoint.firstImageUrl()))
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private static BigDecimal surfacePct(SurfaceBreakdown surface, SurfaceKind surfaceKind) {
        if (surface == null) {
            return decimal(0);
        }

        return switch (surfaceKind) {
            case ASPHALT -> decimal(surface.asphaltPct());
            case DIRT -> decimal(surface.dirtPct());
            case STAIRS -> decimal(surface.stairsPct());
        };
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static Point point(double lng, double lat) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326);
        return point;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private static JsonNode nullIfEmpty(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        if ((value.isObject() || value.isArray()) && value.isEmpty()) {
            return null;
        }
        return value;
    }

    private static String firstImageUrl(JsonNode tourData) {
        if (tourData == null) {
            return null;
        }
        return firstText(tourData.findValue("firstimage"), tourData.findValue("firstImageUrl"));
    }

    private static String firstText(JsonNode primary, JsonNode fallback) {
        if (primary != null && primary.isTextual() && StringUtils.hasText(primary.asText())) {
            return primary.asText();
        }
        if (fallback != null && fallback.isTextual() && StringUtils.hasText(fallback.asText())) {
            return fallback.asText();
        }
        return null;
    }

    private static double distanceMeters(double fromLat, double fromLng, double toLat, double toLng) {
        double latRadians = Math.toRadians(toLat - fromLat);
        double lngRadians = Math.toRadians(toLng - fromLng);
        double haversine = Math.sin(latRadians / 2) * Math.sin(latRadians / 2)
                + Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat))
                * Math.sin(lngRadians / 2) * Math.sin(lngRadians / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private enum SurfaceKind {
        ASPHALT,
        DIRT,
        STAIRS
    }

    private record ResolvedWaypoint(
            WaypointRequest request,
            String tourContentId,
            String tourContentTypeId,
            JsonNode tourData,
            LocalDateTime tourSyncedAt,
            String firstImageUrl,
            String thumbnailImageUrl
    ) {
    }
}
