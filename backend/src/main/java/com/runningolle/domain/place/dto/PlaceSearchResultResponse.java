package com.runningolle.domain.place.dto;

import com.runningolle.domain.place.client.KakaoPlaceClient.KakaoPlace;
import com.runningolle.domain.tourism.entity.TourismPlace;

public record PlaceSearchResultResponse(
        String kakaoPlaceId,
        String name,
        String categoryGroupCode,
        String categoryName,
        String address,
        double lat,
        double lng,
        boolean isTourismCandidate
) {

    public static PlaceSearchResultResponse from(KakaoPlace place, boolean isTourismCandidate) {
        String categoryGroupCode = firstNonBlank(place.categoryGroupCode(), inferredCategoryGroupCode(place.categoryName()));
        return new PlaceSearchResultResponse(
                place.kakaoPlaceId(),
                place.name(),
                categoryGroupCode,
                place.categoryName(),
                firstNonBlank(place.roadAddress(), place.address()),
                place.lat(),
                place.lng(),
                isTourismCandidate || "AT4".equals(categoryGroupCode)
        );
    }

    public static PlaceSearchResultResponse fromOfficialTourism(TourismPlace place) {
        return new PlaceSearchResultResponse(
                "tourapi:" + place.getContentId(),
                place.getTitle(),
                "AT4",
                officialTourismCategoryName(place.getContentTypeId()),
                address(place),
                place.getLocation().getY(),
                place.getLocation().getX(),
                true
        );
    }

    private static String officialTourismCategoryName(String contentTypeId) {
        return switch (contentTypeId) {
            case "14" -> "문화시설";
            case "28" -> "레포츠";
            default -> "관광지";
        };
    }

    private static String address(TourismPlace place) {
        if (place.getAddress() == null || place.getAddress().isBlank()) {
            return place.getDetailAddress();
        }
        if (place.getDetailAddress() == null || place.getDetailAddress().isBlank()) {
            return place.getAddress();
        }
        return place.getAddress() + " " + place.getDetailAddress();
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private static String inferredCategoryGroupCode(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        if (categoryName.contains("관광")
                || categoryName.contains("명소")
                || categoryName.contains("여행")
                || categoryName.contains("산봉우리")
                || categoryName.contains("오름")
                || categoryName.contains("해수욕장")) {
            return "AT4";
        }
        if (categoryName.contains("카페") || categoryName.contains("커피")) {
            return "CE7";
        }
        if (categoryName.contains("음식점")) {
            return "FD6";
        }
        if (categoryName.contains("편의점")) {
            return "CS2";
        }
        if (categoryName.contains("숙박")) {
            return "AD5";
        }
        return null;
    }
}
