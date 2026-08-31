package com.runningolle.domain.place.controller;

import com.runningolle.domain.place.dto.PlaceDetailResponse;
import com.runningolle.domain.place.dto.PlaceSearchResultResponse;
import com.runningolle.domain.place.service.PlaceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/search")
    public List<PlaceSearchResultResponse> searchPlaces(
            @RequestParam String keyword,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Integer radius
    ) {
        return placeService.searchPlaces(keyword, lat, lng, radius);
    }

    @GetMapping("/nearby")
    public List<PlaceSearchResultResponse> searchNearbyPlaces(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Integer radius,
            @RequestParam String categoryGroupCode
    ) {
        return placeService.searchNearbyPlaces(lat, lng, radius, categoryGroupCode);
    }

    @GetMapping("/{kakaoPlaceId}/detail")
    public PlaceDetailResponse getPlaceDetail(
            @PathVariable String kakaoPlaceId,
            @RequestParam String name,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) String categoryGroupCode
    ) {
        return placeService.getPlaceDetail(kakaoPlaceId, name, lat, lng, categoryGroupCode);
    }
}
