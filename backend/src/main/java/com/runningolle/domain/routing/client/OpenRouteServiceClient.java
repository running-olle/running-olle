package com.runningolle.domain.routing.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.runningolle.global.client.ExternalApiRestClientSupport;
import com.runningolle.global.config.properties.ExternalApiProperties;
import com.runningolle.global.exception.ExternalApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenRouteServiceClient {

    private static final String PROVIDER = "OpenRouteService";
    private static final String BASE_URL = "https://api.openrouteservice.org";
    private static final double WAYPOINT_SNAP_RADIUS_METERS = 2_000.0;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final ExternalApiProperties properties;
    private final RestClient restClient;
    private final WKTWriter wktWriter = new WKTWriter();

    public OpenRouteServiceClient(ExternalApiProperties properties) {
        this.properties = properties;
        this.restClient = ExternalApiRestClientSupport.restClient(BASE_URL);
    }

    public OrsRouteResult calculateFootWalkingRoute(List<Waypoint> waypoints) {
        validateRequest(waypoints);

        try {
            OrsDirectionsRequest request = new OrsDirectionsRequest(
                    waypoints.stream()
                            .map(waypoint -> List.of(waypoint.lng(), waypoint.lat()))
                            .toList(),
                    true,
                    List.of("surface", "steepness", "waytype"),
                    new OrsRoutingOptions(List.of("steps")),
                    waypoints.stream()
                            .map(waypoint -> WAYPOINT_SNAP_RADIUS_METERS)
                            .toList(),
                    false
            );

            Map<String, Object> response = restClient.post()
                    .uri("/v2/directions/foot-walking/geojson")
                    .header(HttpHeaders.AUTHORIZATION, properties.getOpenRouteServiceKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, ExternalApiRestClientSupport.errorHandler(PROVIDER))
                    .body(new ParameterizedTypeReference<>() {
                    });

            return parseRouteResult(response);
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalApiException(PROVIDER, "OpenRouteService 경로 계산 호출에 실패했습니다.", exception);
        }
    }

    private void validateRequest(List<Waypoint> waypoints) {
        if (!StringUtils.hasText(properties.getOpenRouteServiceKey())) {
            throw new ExternalApiException(PROVIDER, "OpenRouteService API 키가 설정되지 않았습니다.");
        }
        if (waypoints == null || waypoints.size() < 2) {
            throw new IllegalArgumentException("경로 계산에는 최소 2개 이상의 경유지가 필요합니다.");
        }
    }

    private OrsRouteResult parseRouteResult(Map<String, Object> response) {
        Map<String, Object> feature = extractFirstFeature(response);
        Map<String, Object> propertiesMap = readRequiredMap(feature.get("properties"), "ORS route properties");
        Map<String, Object> summary = readRequiredMap(propertiesMap.get("summary"), "ORS route summary");
        Map<String, Object> geometry = readRequiredMap(feature.get("geometry"), "ORS route geometry");

        Double distanceMeters = readDouble(summary.get("distance"));
        Double durationSeconds = readDouble(summary.get("duration"));
        if (distanceMeters == null || durationSeconds == null) {
            throw new ExternalApiException(PROVIDER, "OpenRouteService 경로 요약 정보가 비어 있습니다.");
        }

        ParsedGeometry parsedGeometry = parseGeometry(geometry);
        SurfaceBreakdown surfaceBreakdown = parseSurfaceBreakdown(propertiesMap, distanceMeters);
        double distanceKm = round(distanceMeters / 1000.0);
        int estimatedDurationMinutes = (int) Math.ceil(durationSeconds / 60.0);
        double elevationGainM = round(parsedGeometry.elevationGainM());

        return new OrsRouteResult(
                distanceKm,
                estimatedDurationMinutes,
                elevationGainM,
                surfaceBreakdown,
                parsedGeometry.routeLineString(),
                wktWriter.write(parsedGeometry.routeLineString()),
                parseSegmentDistanceKm(propertiesMap)
        );
    }

    private static Map<String, Object> extractFirstFeature(Map<String, Object> response) {
        if (response == null) {
            throw new ExternalApiException(PROVIDER, "OpenRouteService 응답이 비어 있습니다.");
        }

        Object featuresValue = response.get("features");
        if (!(featuresValue instanceof List<?> features) || features.isEmpty()) {
            throw new ExternalApiException(PROVIDER, "OpenRouteService 경로 feature가 비어 있습니다.");
        }

        return readRequiredMap(features.get(0), "ORS route feature");
    }

    private static ParsedGeometry parseGeometry(Map<String, Object> geometry) {
        Object coordinatesValue = geometry.get("coordinates");
        if (!(coordinatesValue instanceof List<?> coordinates) || coordinates.size() < 2) {
            throw new ExternalApiException(PROVIDER, "OpenRouteService 경로 좌표가 비어 있습니다.");
        }

        List<Coordinate> routeCoordinates = new ArrayList<>();
        Double previousElevation = null;
        double elevationGainM = 0;

        for (Object coordinateValue : coordinates) {
            if (!(coordinateValue instanceof List<?> coordinate) || coordinate.size() < 2) {
                continue;
            }

            Double lng = readDouble(coordinate.get(0));
            Double lat = readDouble(coordinate.get(1));
            if (lng == null || lat == null) {
                continue;
            }

            routeCoordinates.add(new Coordinate(lng, lat));

            if (coordinate.size() >= 3) {
                Double elevation = readDouble(coordinate.get(2));
                if (elevation != null) {
                    if (previousElevation != null && elevation > previousElevation) {
                        elevationGainM += elevation - previousElevation;
                    }
                    previousElevation = elevation;
                }
            }
        }

        if (routeCoordinates.size() < 2) {
            throw new ExternalApiException(PROVIDER, "OpenRouteService 경로 좌표를 파싱할 수 없습니다.");
        }

        LineString routeLineString = GEOMETRY_FACTORY.createLineString(routeCoordinates.toArray(Coordinate[]::new));
        routeLineString.setSRID(4326);
        return new ParsedGeometry(routeLineString, elevationGainM);
    }

    private static SurfaceBreakdown parseSurfaceBreakdown(Map<String, Object> propertiesMap, double totalDistanceMeters) {
        if (totalDistanceMeters <= 0) {
            return null;
        }

        Map<String, Object> extras = readMap(propertiesMap.get("extras"));
        if (extras == null) {
            return null;
        }

        SurfaceDistance surfaceDistance = new SurfaceDistance();
        accumulateSurfaceSummary(extras.get("surface"), surfaceDistance);
        accumulateWaytypeSummary(firstPresent(extras, "waytypes", "waytype"), surfaceDistance);

        if (!surfaceDistance.hasKnownDistance()) {
            return null;
        }

        return new SurfaceBreakdown(
                round(surfaceDistance.asphaltMeters / totalDistanceMeters * 100.0),
                round(surfaceDistance.dirtMeters / totalDistanceMeters * 100.0),
                round(surfaceDistance.stairsMeters / totalDistanceMeters * 100.0)
        );
    }

    private static List<Double> parseSegmentDistanceKm(Map<String, Object> propertiesMap) {
        Object segmentsValue = propertiesMap.get("segments");
        if (!(segmentsValue instanceof List<?> segments)) {
            return List.of();
        }

        return segments.stream()
                .filter(Map.class::isInstance)
                .map(OpenRouteServiceClient::toStringObjectMap)
                .map(segment -> readDouble(segment.get("distance")))
                .filter(distanceMeters -> distanceMeters != null && distanceMeters >= 0)
                .map(distanceMeters -> round(distanceMeters / 1000.0))
                .toList();
    }

    private static void accumulateSurfaceSummary(Object extraInfoValue, SurfaceDistance surfaceDistance) {
        for (Map<String, Object> item : readSummary(extraInfoValue)) {
            Integer value = readInteger(item.get("value"));
            Double distance = readDouble(item.get("distance"));
            if (value == null || distance == null || distance <= 0) {
                continue;
            }

            // TODO: ORS surface code mapping should be rechecked against official docs before this becomes a core metric.
            if (isAsphaltLikeSurface(value)) {
                surfaceDistance.asphaltMeters += distance;
            } else if (isDirtLikeSurface(value)) {
                surfaceDistance.dirtMeters += distance;
            }
        }
    }

    private static void accumulateWaytypeSummary(Object extraInfoValue, SurfaceDistance surfaceDistance) {
        for (Map<String, Object> item : readSummary(extraInfoValue)) {
            Integer value = readInteger(item.get("value"));
            Double distance = readDouble(item.get("distance"));
            if (value == null || distance == null || distance <= 0) {
                continue;
            }

            // TODO: ORS waytype step code should be rechecked against official docs before this becomes a core metric.
            if (value == 8) {
                surfaceDistance.stairsMeters += distance;
            }
        }
    }

    private static boolean isAsphaltLikeSurface(int value) {
        return switch (value) {
            case 1, 3, 4, 5, 6, 7, 14 -> true;
            default -> false;
        };
    }

    private static boolean isDirtLikeSurface(int value) {
        return switch (value) {
            case 2, 8, 9, 10, 11, 12, 13, 15, 16, 17, 18 -> true;
            default -> false;
        };
    }

    private static List<Map<String, Object>> readSummary(Object extraInfoValue) {
        Map<String, Object> extraInfo = readMap(extraInfoValue);
        if (extraInfo == null || !(extraInfo.get("summary") instanceof List<?> summary)) {
            return List.of();
        }

        return summary.stream()
                .filter(Map.class::isInstance)
                .map(OpenRouteServiceClient::toStringObjectMap)
                .toList();
    }

    private static Object firstPresent(Map<String, Object> source, String firstKey, String secondKey) {
        Object first = source.get(firstKey);
        return first == null ? source.get(secondKey) : first;
    }

    private static Map<String, Object> readRequiredMap(Object value, String label) {
        Map<String, Object> map = readMap(value);
        if (map == null) {
            throw new ExternalApiException(PROVIDER, label + "을 파싱할 수 없습니다.");
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toStringObjectMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static Double readDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer readInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public record Waypoint(
            String name,
            double lat,
            double lng
    ) {
    }

    public record OrsRouteResult(
            double distanceKm,
            int estimatedDurationMinutes,
            double elevationGainM,
            SurfaceBreakdown surface,
            LineString routeLineString,
            String routeLineStringWkt,
            List<Double> segmentDistanceKm
    ) {
    }

    public record SurfaceBreakdown(
            double asphaltPct,
            double dirtPct,
            double stairsPct
    ) {
    }

    private record ParsedGeometry(
            LineString routeLineString,
            double elevationGainM
    ) {
    }

    private record OrsDirectionsRequest(
            List<List<Double>> coordinates,
            Boolean elevation,
            @JsonProperty("extra_info")
            List<String> extraInfo,
            OrsRoutingOptions options,
            List<Double> radiuses,
            Boolean instructions
    ) {
    }

    private record OrsRoutingOptions(
            @JsonProperty("avoid_features")
            List<String> avoidFeatures
    ) {
    }

    private static class SurfaceDistance {

        private double asphaltMeters;
        private double dirtMeters;
        private double stairsMeters;

        private boolean hasKnownDistance() {
            return asphaltMeters > 0 || dirtMeters > 0 || stairsMeters > 0;
        }
    }
}
