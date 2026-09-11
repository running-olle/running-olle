package com.runningolle.domain.tourism.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.domain.tourism.client.TourApiClient;
import com.runningolle.domain.tourism.client.TourApiClient.TourAreaItem;
import com.runningolle.domain.tourism.client.TourApiClient.TourAreaPage;
import com.runningolle.domain.tourism.client.TourApiClient.TourDetail;
import com.runningolle.domain.tourism.config.TourismEventSyncProperties;
import com.runningolle.domain.tourism.dto.TourismEventSyncResponse;
import com.runningolle.domain.tourism.entity.TourismEvent;
import com.runningolle.domain.tourism.entity.TourismEvent.TourismEventSnapshot;
import com.runningolle.domain.tourism.repository.TourismEventRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
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
public class TourismEventSyncService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final DateTimeFormatter TOUR_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String FESTIVAL_CONTENT_TYPE_ID = "15";
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final TourApiClient tourApiClient;
    private final TourismEventRepository tourismEventRepository;
    private final TourismEventSyncProperties tourismEventSyncProperties;
    private final ObjectMapper objectMapper;

    public TourismEventSyncResponse syncJejuTourismEvents() {
        LocalDateTime syncedAt = LocalDateTime.now();
        SyncStats stats = new SyncStats();
        int pageNo = 1;
        int pageSize = Math.max(1, tourismEventSyncProperties.getPageSize());

        while (true) {
            TourAreaPage page = tourApiClient.getAreaBasedList(
                    tourismEventSyncProperties.getAreaCode(),
                    FESTIVAL_CONTENT_TYPE_ID,
                    pageNo,
                    pageSize
            );

            if (page.items().isEmpty()) {
                break;
            }

            for (TourAreaItem item : page.items()) {
                syncItem(item, stats, syncedAt);
            }

            if (page.pageNo() * page.numOfRows() >= page.totalCount()) {
                break;
            }
            pageNo++;
        }

        return new TourismEventSyncResponse(
                tourismEventSyncProperties.getAreaCode(),
                stats.fetchedCount,
                stats.createdCount,
                stats.updatedCount,
                stats.skippedCount,
                stats.failedCount,
                syncedAt
        );
    }

    private void syncItem(TourAreaItem item, SyncStats stats, LocalDateTime syncedAt) {
        stats.fetchedCount++;

        if (!isSyncable(item)) {
            stats.skippedCount++;
            return;
        }

        Optional<TourDetail> detail = fetchDetail(item, stats);

        try {
            TourismEventSnapshot snapshot = toSnapshot(item, detail.orElse(null), syncedAt);
            if (snapshot.eventStartDate() == null || snapshot.eventEndDate() == null) {
                stats.skippedCount++;
                return;
            }

            Optional<TourismEvent> savedEvent = tourismEventRepository.findByContentId(item.contentId());

            if (savedEvent.isPresent()) {
                savedEvent.get().sync(snapshot);
                tourismEventRepository.save(savedEvent.get());
                stats.updatedCount++;
            } else {
                tourismEventRepository.save(TourismEvent.create(snapshot));
                stats.createdCount++;
            }
        } catch (RuntimeException exception) {
            stats.failedCount++;
            log.warn(
                    "Failed to sync TourAPI event. contentId={}, title={}",
                    item.contentId(),
                    item.title(),
                    exception
            );
        }
    }

    private Optional<TourDetail> fetchDetail(TourAreaItem item, SyncStats stats) {
        try {
            return tourApiClient.getDetail(item.contentId(), FESTIVAL_CONTENT_TYPE_ID);
        } catch (RuntimeException exception) {
            stats.failedCount++;
            log.warn(
                    "Failed to enrich TourAPI event detail. contentId={}, title={}",
                    item.contentId(),
                    item.title(),
                    exception
            );
            return Optional.empty();
        }
    }

    private TourismEventSnapshot toSnapshot(TourAreaItem item, TourDetail detail, LocalDateTime syncedAt) {
        Map<String, Object> introRaw = introRaw(detail);
        LocalDate eventStartDate = firstParsedDate(readString(introRaw, "eventstartdate"));
        LocalDate eventEndDate = firstParsedDate(readString(introRaw, "eventenddate"), readString(introRaw, "eventstartdate"));
        double lat = detail == null || detail.lat() == null ? item.lat() : detail.lat();
        double lng = detail == null || detail.lng() == null ? item.lng() : detail.lng();
        String overview = cleanText(detail == null ? null : detail.overview());
        String venueName = firstNonBlank(readString(introRaw, "eventplace"), item.address());
        String organizer = firstNonBlank(readString(introRaw, "sponsor1"), readString(introRaw, "sponsor2"));
        String homepage = readString(introRaw, "eventhomepage");
        String tel = firstNonBlank(item.tel(), readString(introRaw, "sponsor1tel"));
        RunningScore runningScore = calculateRunningScore(item, overview, venueName, organizer);

        return new TourismEventSnapshot(
                item.contentId(),
                firstNonBlank(detail == null ? null : detail.contentTypeId(), item.contentTypeId()),
                firstNonBlank(detail == null ? null : detail.title(), item.title()),
                firstNonBlank(detail == null ? null : detail.address(), item.address()),
                firstNonBlank(detail == null ? null : detail.detailAddress(), item.detailAddress()),
                cleanText(venueName),
                cleanText(organizer),
                cleanText(tel),
                homepage,
                eventStartDate,
                eventEndDate,
                firstNonBlank(detail == null ? null : detail.category1(), item.category1()),
                firstNonBlank(detail == null ? null : detail.category2(), item.category2()),
                firstNonBlank(detail == null ? null : detail.category3(), item.category3()),
                firstNonBlank(detail == null ? null : detail.areaCode(), item.areaCode()),
                firstNonBlank(detail == null ? null : detail.sigunguCode(), item.sigunguCode()),
                point(lng, lat),
                firstNonBlank(detail == null ? null : detail.firstImageUrl(), item.firstImageUrl()),
                item.thumbnailImageUrl(),
                overview,
                runningScore.related(),
                runningScore.score(),
                item.createdTime(),
                item.modifiedTime(),
                rawData(item, detail),
                syncedAt
        );
    }

    private JsonNode rawData(TourAreaItem item, TourDetail detail) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("areaBasedList2", item.raw());
        if (detail != null) {
            raw.put("detail", detail.raw());
        }
        return objectMapper.valueToTree(raw);
    }

    private RunningScore calculateRunningScore(TourAreaItem item, String overview, String venueName, String organizer) {
        String searchable = String.join(
                " ",
                nullToBlank(item.title()),
                nullToBlank(item.address()),
                nullToBlank(overview),
                nullToBlank(venueName),
                nullToBlank(organizer)
        ).toLowerCase();
        int score = tourismEventSyncProperties.getRunningKeywords().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(String::toLowerCase)
                .distinct()
                .mapToInt(keyword -> searchable.contains(keyword) ? keywordWeight(keyword) : 0)
                .sum();
        return new RunningScore(score >= 3, score);
    }

    private static int keywordWeight(String keyword) {
        return switch (keyword) {
            case "마라톤", "러닝", "트레일런", "레이스" -> 5;
            case "런", "달리기", "트레일" -> 3;
            default -> 1;
        };
    }

    private static boolean isSyncable(TourAreaItem item) {
        return StringUtils.hasText(item.contentId())
                && StringUtils.hasText(item.title())
                && item.lat() != null
                && item.lng() != null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> introRaw(TourDetail detail) {
        if (detail == null || detail.raw() == null) {
            return Map.of();
        }
        Object intro = detail.raw().get("detailIntro2");
        if (intro instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static LocalDate firstParsedDate(String... values) {
        for (String value : values) {
            LocalDate parsedDate = parseDate(value);
            if (parsedDate != null) {
                return parsedDate;
            }
        }
        return null;
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), TOUR_DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String readString(Map<String, Object> raw, String key) {
        Object value = raw.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private static Point point(double lng, double lat) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326);
        return point;
    }

    private static String cleanText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String withoutBreaks = value
                .replace("<br>", "\n")
                .replace("<br/>", "\n")
                .replace("<br />", "\n");
        String text = HTML_TAG_PATTERN.matcher(withoutBreaks).replaceAll(" ");
        return WHITESPACE_PATTERN.matcher(text).replaceAll(" ").trim();
    }

    private static String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private static class SyncStats {

        private int fetchedCount;
        private int createdCount;
        private int updatedCount;
        private int skippedCount;
        private int failedCount;
    }

    private record RunningScore(boolean related, int score) {
    }
}
