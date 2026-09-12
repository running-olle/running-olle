package com.runningolle.domain.safety.dto;

public record SafetyNearbyPlaceResponse(
        String type,
        String name,
        String categoryName,
        String address,
        double lat,
        double lng,
        Integer distanceMeters,
        String phone,
        String placeUrl
) {
}
