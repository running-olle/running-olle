package com.runningolle.domain.safety.service;

import com.runningolle.domain.place.client.KakaoPlaceClient;
import com.runningolle.domain.place.client.KakaoPlaceClient.KakaoPlace;
import com.runningolle.domain.safety.dto.SafetyNearbyPlaceResponse;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SafetyPlaceService {

    private static final int DEFAULT_RADIUS_METERS = 1_500;
    private static final int MAX_RADIUS_METERS = 5_000;
    private static final int PER_TYPE_LIMIT = 3;

    private final KakaoPlaceClient kakaoPlaceClient;

    public List<SafetyNearbyPlaceResponse> findNearbySafetyPlaces(double lat, double lng, Integer radiusMeters) {
        validateCoordinate(lat, lng);
        int radius = normalizeRadius(radiusMeters);

        return List.of(
                        search("hospital", "\uBCD1\uC6D0", "HP8", lat, lng, radius),
                        search("pharmacy", "\uC57D\uAD6D", "PM9", lat, lng, radius),
                        search("convenience_store", "\uD3B8\uC758\uC810", "CS2", lat, lng, radius),
                        search("toilet", "\uD654\uC7A5\uC2E4", null, lat, lng, radius)
                ).stream()
                .flatMap(List::stream)
                .sorted(Comparator
                        .comparingInt((SafetyNearbyPlaceResponse place) ->
                                place.distanceMeters() == null ? Integer.MAX_VALUE : place.distanceMeters())
                        .thenComparing(SafetyNearbyPlaceResponse::name))
                .toList();
    }

    private List<SafetyNearbyPlaceResponse> search(
            String type,
            String keyword,
            String categoryGroupCode,
            double lat,
            double lng,
            int radius
    ) {
        return kakaoPlaceClient.searchKeyword(keyword, lat, lng, radius, categoryGroupCode).stream()
                .limit(PER_TYPE_LIMIT)
                .map(place -> toResponse(type, place))
                .toList();
    }

    private static SafetyNearbyPlaceResponse toResponse(String type, KakaoPlace place) {
        return new SafetyNearbyPlaceResponse(
                type,
                place.name(),
                place.categoryName(),
                firstNonBlank(place.roadAddress(), place.address()),
                place.lat(),
                place.lng(),
                place.distanceMeters(),
                place.phone(),
                place.placeUrl()
        );
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }

    private static void validateCoordinate(double lat, double lng) {
        if (Double.isNaN(lat) || Double.isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid coordinates.");
        }
    }

    private static int normalizeRadius(Integer radiusMeters) {
        if (radiusMeters == null) {
            return DEFAULT_RADIUS_METERS;
        }
        if (radiusMeters <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Radius must be positive.");
        }
        return Math.min(radiusMeters, MAX_RADIUS_METERS);
    }
}
