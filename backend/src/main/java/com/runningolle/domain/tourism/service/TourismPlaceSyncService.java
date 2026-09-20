package com.runningolle.domain.tourism.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.domain.tourism.client.TourApiClient;
import com.runningolle.domain.tourism.client.TourApiClient.TourAreaItem;
import com.runningolle.domain.tourism.client.TourApiClient.TourAreaPage;
import com.runningolle.domain.tourism.config.TourismSyncProperties;
import com.runningolle.domain.tourism.dto.TourismSyncResponse;
import com.runningolle.domain.tourism.entity.TourismPlace;
import com.runningolle.domain.tourism.entity.TourismPlace.TourismPlaceSnapshot;
import com.runningolle.domain.tourism.repository.TourismPlaceRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourismPlaceSyncService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final TourApiClient tourApiClient;
    private final TourismPlaceRepository tourismPlaceRepository;
    private final TourismSyncProperties tourismSyncProperties;
    private final ObjectMapper objectMapper;

    public TourismSyncResponse syncJejuTourismPlaces() {
        LocalDateTime syncedAt = LocalDateTime.now();
        SyncStats stats = new SyncStats();
        List<String> contentTypeIds = targetContentTypeIds();

        for (String contentTypeId : contentTypeIds) {
            syncContentType(contentTypeId, stats, syncedAt);
        }

        return new TourismSyncResponse(
                tourismSyncProperties.getAreaCode(),
                contentTypeIds,
                stats.fetchedCount,
                stats.createdCount,
                stats.updatedCount,
                stats.skippedCount,
                stats.failedCount,
                syncedAt
        );
    }

    private List<String> targetContentTypeIds() {
        return tourismSyncProperties.getContentTypeIds().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private void syncContentType(String contentTypeId, SyncStats stats, LocalDateTime syncedAt) {
        int pageNo = 1;
        int pageSize = Math.max(1, tourismSyncProperties.getPageSize());

        while (true) {
            TourAreaPage page = tourApiClient.getAreaBasedList(
                    tourismSyncProperties.getAreaCode(),
                    contentTypeId,
                    pageNo,
                    pageSize
            );

            if (page.items().isEmpty()) {
                return;
            }

            for (TourAreaItem item : page.items()) {
                syncItem(item, stats, syncedAt);
            }

            if (page.pageNo() * page.numOfRows() >= page.totalCount()) {
                return;
            }
            pageNo++;
        }
    }

    private void syncItem(TourAreaItem item, SyncStats stats, LocalDateTime syncedAt) {
        stats.fetchedCount++;

        if (!isSyncable(item)) {
            stats.skippedCount++;
            return;
        }

        try {
            Optional<TourismPlace> savedPlace = tourismPlaceRepository.findByContentId(item.contentId());
            TourismPlaceSnapshot snapshot = toInventorySnapshot(item, savedPlace.orElse(null), syncedAt);

            if (savedPlace.isPresent()) {
                savedPlace.get().syncInventory(snapshot);
                tourismPlaceRepository.save(savedPlace.get());
                stats.updatedCount++;
            } else {
                tourismPlaceRepository.save(TourismPlace.createFromInventory(snapshot));
                stats.createdCount++;
            }
        } catch (RuntimeException exception) {
            stats.failedCount++;
            log.warn(
                    "Failed to sync TourAPI place. contentId={}, title={}",
                    item.contentId(),
                    item.title(),
                    exception
            );
        }
    }

    private static boolean isSyncable(TourAreaItem item) {
        return StringUtils.hasText(item.contentId())
                && StringUtils.hasText(item.contentTypeId())
                && StringUtils.hasText(item.title())
                && item.lat() != null
                && item.lng() != null;
    }

    private TourismPlaceSnapshot toInventorySnapshot(
            TourAreaItem item,
            TourismPlace existingPlace,
            LocalDateTime syncedAt
    ) {
        return new TourismPlaceSnapshot(
                item.contentId(),
                item.contentTypeId(),
                item.title(),
                item.address(),
                item.detailAddress(),
                item.tel(),
                item.category1(),
                item.category2(),
                item.category3(),
                item.areaCode(),
                item.sigunguCode(),
                point(item.lng(), item.lat()),
                item.firstImageUrl(),
                item.thumbnailImageUrl(),
                existingPlace == null ? null : existingPlace.getOverview(),
                existingPlace == null ? null : existingPlace.getUseTime(),
                item.createdTime(),
                item.modifiedTime(),
                inventoryRawData(item, existingPlace),
                syncedAt
        );
    }

    private JsonNode inventoryRawData(TourAreaItem item, TourismPlace existingPlace) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("areaBasedList2", item.raw());
        if (existingPlace != null
                && existingPlace.getRawData() != null
                && existingPlace.getRawData().has("detail")) {
            raw.put("detail", existingPlace.getRawData().get("detail"));
        }
        return objectMapper.valueToTree(raw);
    }

    private static Point point(double lng, double lat) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326);
        return point;
    }

    private static class SyncStats {

        private int fetchedCount;
        private int createdCount;
        private int updatedCount;
        private int skippedCount;
        private int failedCount;
    }
}
