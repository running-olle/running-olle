package com.runningolle.domain.tourism.dto;

import java.time.LocalDate;

public record TourismEventResponse(
        String id,
        String contentId,
        String contentTypeId,
        String title,
        String address,
        String detailAddress,
        String venueName,
        String organizer,
        String tel,
        String homepage,
        LocalDate eventStartDate,
        LocalDate eventEndDate,
        String status,
        long dday,
        String categoryLabel,
        boolean runningRelated,
        int runningScore,
        Double lat,
        Double lng,
        String firstImageUrl,
        String thumbnailImageUrl,
        String overview,
        String providerName,
        String sourceUrl
) {
}
