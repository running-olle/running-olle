package com.runningolle.domain.safety.controller;

import com.runningolle.domain.safety.dto.SafetyNearbyPlaceResponse;
import com.runningolle.domain.safety.service.SafetyPlaceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/safety")
public class SafetyController {

    private final SafetyPlaceService safetyPlaceService;

    @GetMapping("/nearby")
    public List<SafetyNearbyPlaceResponse> findNearbySafetyPlaces(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Integer radius
    ) {
        return safetyPlaceService.findNearbySafetyPlaces(lat, lng, radius);
    }
}
