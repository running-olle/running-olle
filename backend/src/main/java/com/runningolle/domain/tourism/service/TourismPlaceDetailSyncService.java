package com.runningolle.domain.tourism.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.domain.tourism.client.TourApiClient;
import com.runningolle.domain.tourism.client.TourApiClient.TourDetail;
import com.runningolle.domain.tourism.config.TourismSyncProperties;
import com.runningolle.domain.tourism.dto.TourismDetailSyncResponse;
import com.runningolle.domain.tourism.entity.TourismPlace;
import com.runningolle.domain.tourism.entity.TourismPlace.TourismPlaceDetailSnapshot;
import com.runningolle.domain.tourism.repository.TourismPlaceRepository;
import com.runningolle.global.exception.ExternalApiException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourismPlaceDetailSyncService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final TourApiClient tourApiClient;
    private final TourismPlaceRepository tourismPlaceRepository;
    private final TourismSyncProperties tourismSyncProperties;
    private final ObjectMapper objectMapper;

    public TourismDetailSyncResponse syncPendingDetails() {
        LocalDateTime processedAt = LocalDateTime.now();
        var candidates = tourismPlaceRepository.findDetailSyncCandidates(
                processedAt,
                Math.max(1, tourismSyncProperties.getDetailMaxRetries()),
                Math.max(1, tourismSyncProperties.getDetailBatchSize())
        );
        int completedCount = 0;
        int failedCount = 0;
        int processedCount = 0;

        for (TourismPlace place : candidates) {
            processedCount++;
            try {
                TourDetail detail = tourApiClient.getDetail(place.getContentId(), place.getContentTypeId())
                        .orElse(null);
                place.completeDetailSync(toSnapshot(place, detail), processedAt);
                tourismPlaceRepository.save(place);
                completedCount++;
            } catch (RuntimeException exception) {
                place.failDetailSync(
                        exception.getMessage(),
                        processedAt.plusHours(Math.max(1, tourismSyncProperties.getDetailRetryDelayHours()))
                );
                tourismPlaceRepository.save(place);
                failedCount++;
                log.warn(
                        "Failed to enrich TourAPI place detail. contentId={}, title={}, retryCount={}",
                        place.getContentId(),
                        place.getTitle(),
                        place.getDetailRetryCount(),
                        exception
                );
                if (isQuotaExceeded(exception)) {
                    log.error("TourAPI quota was exceeded. Remaining detail candidates are deferred.");
                    break;
                }
            }
        }

        return new TourismDetailSyncResponse(
                candidates.size(),
                processedCount,
                completedCount,
                failedCount,
                candidates.size() - processedCount,
                processedAt
        );
    }

    private TourismPlaceDetailSnapshot toSnapshot(TourismPlace place, TourDetail detail) {
        if (detail == null) {
            return new TourismPlaceDetailSnapshot(
                    null, null, null, null, null, null, null, null,
                    null, null, null, null, place.getRawData()
            );
        }

        return new TourismPlaceDetailSnapshot(
                detail.title(),
                detail.address(),
                detail.detailAddress(),
                detail.category1(),
                detail.category2(),
                detail.category3(),
                detail.areaCode(),
                detail.sigunguCode(),
                point(detail.lng(), detail.lat()),
                detail.firstImageUrl(),
                detail.overview(),
                detail.useTime(),
                detailRawData(place.getRawData(), detail)
        );
    }

    private JsonNode detailRawData(JsonNode existingRawData, TourDetail detail) {
        Map<String, Object> raw = new LinkedHashMap<>();
        if (existingRawData != null && existingRawData.has("areaBasedList2")) {
            raw.put("areaBasedList2", existingRawData.get("areaBasedList2"));
        }
        raw.put("detail", detail.raw());
        return objectMapper.valueToTree(raw);
    }

    private static Point point(Double lng, Double lat) {
        if (lng == null || lat == null) {
            return null;
        }
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326);
        return point;
    }

    private static boolean isQuotaExceeded(RuntimeException exception) {
        if (exception instanceof ExternalApiException externalApiException
                && externalApiException.getUpstreamStatusCode() != null
                && externalApiException.getUpstreamStatusCode() == 429) {
            return true;
        }
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String normalizedMessage = message.toUpperCase();
        return normalizedMessage.contains("QUOTA")
                || normalizedMessage.contains("RATE LIMIT")
                || normalizedMessage.contains("LIMITED_NUMBER")
                || normalizedMessage.contains("SERVICE_REQUESTS_EXCEEDS");
    }
}
