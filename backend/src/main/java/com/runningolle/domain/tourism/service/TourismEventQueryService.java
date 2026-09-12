package com.runningolle.domain.tourism.service;

import com.runningolle.domain.tourism.dto.TourismEventResponse;
import com.runningolle.domain.tourism.dto.TourismEventStatus;
import com.runningolle.domain.tourism.dto.TourismEventTypeFilter;
import com.runningolle.domain.tourism.entity.TourismEvent;
import com.runningolle.domain.tourism.repository.TourismEventRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TourismEventQueryService {

    private static final String DEFAULT_PROVIDER_NAME = "한국관광공사";

    private final TourismEventRepository tourismEventRepository;

    public List<TourismEventResponse> getHighlights(int limit) {
        LocalDate today = LocalDate.now();
        return activeEvents(today).stream()
                .sorted(highlightComparator(today))
                .limit(Math.max(1, Math.min(limit, 10)))
                .map(event -> toResponse(event, today))
                .toList();
    }

    public List<TourismEventResponse> getEvents(
            TourismEventTypeFilter typeFilter,
            TourismEventStatus status,
            String keyword
    ) {
        LocalDate today = LocalDate.now();
        String normalizedKeyword = normalize(keyword);

        return activeEvents(today).stream()
                .filter(event -> matchesType(event, typeFilter))
                .filter(event -> matchesStatus(event, status, today))
                .filter(event -> matchesKeyword(event, normalizedKeyword))
                .sorted(highlightComparator(today))
                .map(event -> toResponse(event, today))
                .toList();
    }

    public TourismEventResponse getEvent(UUID eventId) {
        TourismEvent event = tourismEventRepository.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."));
        return toResponse(event, LocalDate.now());
    }

    private List<TourismEvent> activeEvents(LocalDate today) {
        return tourismEventRepository.findByIsDeletedFalseAndEventEndDateGreaterThanEqual(today);
    }

    private static Comparator<TourismEvent> highlightComparator(LocalDate today) {
        return Comparator
                .comparing((TourismEvent event) -> !Boolean.TRUE.equals(event.getRunningRelated()))
                .thenComparingInt(event -> statusPriority(event, today))
                .thenComparing(event -> dateDistance(event, today))
                .thenComparing(TourismEvent::getTitle);
    }

    private static boolean matchesType(TourismEvent event, TourismEventTypeFilter typeFilter) {
        return switch (typeFilter == null ? TourismEventTypeFilter.ALL : typeFilter) {
            case ALL -> true;
            case RUNNING -> Boolean.TRUE.equals(event.getRunningRelated());
            case FESTIVAL -> "A0207".equals(event.getCategory2());
            case PERFORMANCE -> "A0208".equals(event.getCategory2());
        };
    }

    private static boolean matchesStatus(TourismEvent event, TourismEventStatus status, LocalDate today) {
        return switch (status == null ? TourismEventStatus.ALL : status) {
            case ALL -> true;
            case ONGOING -> !event.getEventStartDate().isAfter(today) && !event.getEventEndDate().isBefore(today);
            case UPCOMING -> event.getEventStartDate().isAfter(today);
        };
    }

    private static boolean matchesKeyword(TourismEvent event, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return contains(event.getTitle(), keyword)
                || contains(event.getAddress(), keyword)
                || contains(event.getVenueName(), keyword)
                || contains(event.getOrganizer(), keyword);
    }

    private static boolean contains(String source, String keyword) {
        return StringUtils.hasText(source) && source.toLowerCase().contains(keyword);
    }

    private static TourismEventResponse toResponse(TourismEvent event, LocalDate today) {
        return new TourismEventResponse(
                event.getId().toString(),
                event.getContentId(),
                event.getContentTypeId(),
                event.getTitle(),
                event.getAddress(),
                event.getDetailAddress(),
                event.getVenueName(),
                event.getOrganizer(),
                event.getTel(),
                event.getHomepage(),
                event.getEventStartDate(),
                event.getEventEndDate(),
                statusLabel(event, today),
                dday(event, today),
                categoryLabel(event),
                Boolean.TRUE.equals(event.getRunningRelated()),
                event.getRunningScore() == null ? 0 : event.getRunningScore(),
                event.getLocation() == null ? null : event.getLocation().getY(),
                event.getLocation() == null ? null : event.getLocation().getX(),
                event.getFirstImageUrl(),
                event.getThumbnailImageUrl(),
                event.getOverview(),
                StringUtils.hasText(event.getProviderName()) ? event.getProviderName() : DEFAULT_PROVIDER_NAME,
                event.getSourceUrl()
        );
    }

    private static String statusLabel(TourismEvent event, LocalDate today) {
        if (!event.getEventStartDate().isAfter(today) && !event.getEventEndDate().isBefore(today)) {
            return "진행 중";
        }
        if (event.getEventEndDate().isBefore(today)) {
            return "종료";
        }
        return "예정";
    }

    private static long dday(TourismEvent event, LocalDate today) {
        if (event.getEventEndDate().isBefore(today)) {
            return ChronoUnit.DAYS.between(event.getEventEndDate(), today);
        }
        return ChronoUnit.DAYS.between(today, event.getEventStartDate());
    }

    private static int statusPriority(TourismEvent event, LocalDate today) {
        if (!event.getEventStartDate().isAfter(today) && !event.getEventEndDate().isBefore(today)) {
            return 0;
        }
        if (event.getEventStartDate().isAfter(today)) {
            return 1;
        }
        return 2;
    }

    private static long dateDistance(TourismEvent event, LocalDate today) {
        if (event.getEventEndDate().isBefore(today)) {
            return ChronoUnit.DAYS.between(event.getEventEndDate(), today);
        }
        return Math.abs(ChronoUnit.DAYS.between(today, event.getEventStartDate()));
    }

    private static String categoryLabel(TourismEvent event) {
        if (Boolean.TRUE.equals(event.getRunningRelated())) {
            return "러닝 행사";
        }
        if ("A0207".equals(event.getCategory2())) {
            return "축제";
        }
        if ("A0208".equals(event.getCategory2())) {
            return "공연·행사";
        }
        return "제주 행사";
    }

    private static String normalize(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase();
    }
}
