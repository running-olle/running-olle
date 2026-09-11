package com.runningolle.domain.tourism.controller;

import com.runningolle.domain.tourism.dto.TourismEventResponse;
import com.runningolle.domain.tourism.dto.TourismEventStatus;
import com.runningolle.domain.tourism.dto.TourismEventTypeFilter;
import com.runningolle.domain.tourism.service.TourismEventQueryService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tourism-events")
public class TourismEventController {

    private final TourismEventQueryService tourismEventQueryService;

    @GetMapping("/highlights")
    public List<TourismEventResponse> getHighlights(
            @RequestParam(defaultValue = "3") int limit
    ) {
        return tourismEventQueryService.getHighlights(limit);
    }

    @GetMapping
    public List<TourismEventResponse> getEvents(
            @RequestParam(defaultValue = "ALL") TourismEventTypeFilter type,
            @RequestParam(defaultValue = "ALL") TourismEventStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return tourismEventQueryService.getEvents(type, status, keyword);
    }

    @GetMapping("/{eventId}")
    public TourismEventResponse getEvent(
            @PathVariable UUID eventId
    ) {
        return tourismEventQueryService.getEvent(eventId);
    }
}
