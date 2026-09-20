package com.runningolle.domain.course.service;

import com.runningolle.domain.course.dto.CourseDraftRouteRequest;
import com.runningolle.domain.course.dto.CourseDraftRouteResponse;
import com.runningolle.domain.routing.client.OpenRouteServiceClient;
import com.runningolle.domain.routing.client.OpenRouteServiceClient.OrsRouteResult;
import com.runningolle.domain.routing.client.OpenRouteServiceClient.Waypoint;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CourseDraftRouteService {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final double CONSECUTIVE_WAYPOINT_TOLERANCE_METERS = 5.0;

    private final OpenRouteServiceClient openRouteServiceClient;

    public CourseDraftRouteResponse calculateDraftRoute(CourseDraftRouteRequest request) {
        List<Waypoint> waypoints = toRoutingWaypoints(request);
        OrsRouteResult routeResult = openRouteServiceClient.calculateFootWalkingRoute(waypoints);

        return CourseDraftRouteResponse.from(
                routeResult,
                CourseDifficultyCalculator.suggest(routeResult.distanceKm(), routeResult.elevationGainM())
        );
    }

    private List<Waypoint> toRoutingWaypoints(CourseDraftRouteRequest request) {
        if (request == null || request.waypoints() == null || request.waypoints().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "경로 계산에는 최소 2개 이상의 경유지가 필요합니다.");
        }

        List<CourseDraftRouteRequest.WaypointRequest> sortedWaypoints = request.waypoints().stream()
                .sorted(Comparator.comparing(CourseDraftRouteRequest.WaypointRequest::orderIndex))
                .toList();
        validateConsecutiveWaypoints(sortedWaypoints);

        return sortedWaypoints.stream()
                .map(waypoint -> new Waypoint(
                        waypoint.name().trim(),
                        waypoint.lat(),
                        waypoint.lng()
                ))
                .toList();
    }

    private static void validateConsecutiveWaypoints(List<CourseDraftRouteRequest.WaypointRequest> waypoints) {
        for (int index = 1; index < waypoints.size(); index++) {
            var previous = waypoints.get(index - 1);
            var current = waypoints.get(index);
            boolean samePlaceId = previous.kakaoPlaceId() != null
                    && !previous.kakaoPlaceId().isBlank()
                    && previous.kakaoPlaceId().equals(current.kakaoPlaceId());
            boolean samePosition = distanceMeters(previous.lat(), previous.lng(), current.lat(), current.lng())
                    <= CONSECUTIVE_WAYPOINT_TOLERANCE_METERS;
            if (samePlaceId || samePosition) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "같은 경유지를 연속으로 추가할 수 없습니다. 다른 경유지를 사이에 추가해 주세요."
                );
            }
        }
    }

    private static double distanceMeters(double fromLat, double fromLng, double toLat, double toLng) {
        double latRadians = Math.toRadians(toLat - fromLat);
        double lngRadians = Math.toRadians(toLng - fromLng);
        double haversine = Math.sin(latRadians / 2) * Math.sin(latRadians / 2)
                + Math.cos(Math.toRadians(fromLat)) * Math.cos(Math.toRadians(toLat))
                * Math.sin(lngRadians / 2) * Math.sin(lngRadians / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

}
