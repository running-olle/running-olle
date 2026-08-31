package com.runningolle.domain.place.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.runningolle.domain.place.client.KakaoPlaceClient;
import com.runningolle.domain.place.client.KakaoPlaceClient.KakaoPlace;
import com.runningolle.domain.place.dto.PlaceDetailResponse;
import com.runningolle.domain.place.dto.PlaceSearchResultResponse;
import com.runningolle.domain.tourism.entity.TourismPlace;
import com.runningolle.domain.tourism.repository.TourismPlaceRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private static final String TOURISM_CATEGORY_GROUP_CODE = "AT4";
    private static final String TOUR_API_PLACE_ID_PREFIX = "tourapi:";
    private static final int DEFAULT_SEARCH_RADIUS_METERS = 5_000;
    private static final int DEFAULT_NEARBY_RADIUS_METERS = 1_500;
    private static final int MAX_SEARCH_RADIUS_METERS = 20_000;
    private static final int DETAIL_SEARCH_RADIUS_METERS = 1_000;
    private static final int TOURISM_SEARCH_LIMIT = 10;
    private static final int PLACE_SEARCH_RESULT_LIMIT = 15;
    private static final int NEARBY_SEARCH_RESULT_LIMIT = 12;
    private static final double TOURISM_MATCH_RADIUS_METERS = 2_000.0;
    private static final double TOURISM_SEARCH_DEDUPLICATE_RADIUS_METERS = 250.0;
    private static final double TOURISM_CLUSTER_DEDUPLICATE_RADIUS_METERS = 2_000.0;
    private static final int TOURISM_MATCH_LIMIT = 20;
    private static final double MIN_TOURISM_NAME_SCORE = 0.55;
    private static final double MIN_TOURISM_DUPLICATE_NAME_SCORE = 0.70;
    private static final double MIN_TOURISM_CLUSTER_NAME_SCORE = 0.85;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final List<String> CATEGORY_ALIASES = List.of(
            "게스트하우스", "관광지", "음식점", "베이커리", "편의점",
            "해산물", "디저트", "전망대", "숙박", "호텔",
            "펜션", "리조트", "관광", "명소", "여행",
            "오름", "해변", "숲길", "맛집", "식당",
            "밥집", "고기", "카페", "커피", "마트",
            "상점", "숙소"
    );

    private final KakaoPlaceClient kakaoPlaceClient;
    private final TourismPlaceRepository tourismPlaceRepository;

    public List<PlaceSearchResultResponse> searchPlaces(String keyword, double lat, double lng, Integer radiusMeters) {
        validateKeyword(keyword);
        validateCoordinate(lat, lng);
        int radius = normalizeRadius(radiusMeters);

        List<KakaoPlace> nearbyKakaoPlaces = relevantKakaoPlaces(
                keyword,
                kakaoPlaceClient.searchKeyword(keyword, lat, lng, radius)
        );
        List<KakaoPlace> kakaoPlaces = nearbyKakaoPlaces.isEmpty()
                ? relevantKakaoPlaces(keyword, kakaoPlaceClient.searchKeywordInJeju(keyword))
                : nearbyKakaoPlaces;
        List<PlaceSearchResultResponse> kakaoResults = kakaoPlaces.stream()
                .map(place -> PlaceSearchResultResponse.from(place, isTourismCandidate(resolvedCategoryGroupCode(place))))
                .toList();
        List<PlaceSearchResultResponse> officialTourismResults = searchOfficialTourismPlaces(keyword, lat, lng, radius).stream()
                .filter(place -> !hasDuplicateKakaoPlace(place, kakaoPlaces))
                .map(PlaceSearchResultResponse::fromOfficialTourism)
                .toList();

        return rankedSearchResults(keyword, lat, lng, kakaoResults, officialTourismResults);
    }

    public List<PlaceSearchResultResponse> searchNearbyPlaces(
            double lat,
            double lng,
            Integer radiusMeters,
            String categoryGroupCode
    ) {
        validateCoordinate(lat, lng);
        String normalizedCategoryGroupCode = normalizeCategoryGroupCode(categoryGroupCode);
        int radius = radiusMeters == null ? DEFAULT_NEARBY_RADIUS_METERS : normalizeRadius(radiusMeters);
        String categoryKeyword = categoryKeyword(normalizedCategoryGroupCode);

        List<KakaoPlace> kakaoPlaces = kakaoPlaceClient.searchKeyword(
                categoryKeyword,
                lat,
                lng,
                radius,
                normalizedCategoryGroupCode
        );
        List<PlaceSearchResultResponse> kakaoResults = kakaoPlaces.stream()
                .map(place -> PlaceSearchResultResponse.from(place, isTourismCandidate(resolvedCategoryGroupCode(place))))
                .toList();

        List<PlaceSearchResultResponse> officialTourismResults = List.of();
        if (isTourismCandidate(normalizedCategoryGroupCode)) {
            officialTourismResults = tourismPlaceRepository.findNearbyOfficialTourismPlaces(
                            lat,
                            lng,
                            radius,
                            NEARBY_SEARCH_RESULT_LIMIT
                    ).stream()
                    .filter(place -> !hasDuplicateKakaoPlace(place, kakaoPlaces))
                    .map(PlaceSearchResultResponse::fromOfficialTourism)
                    .toList();
        }

        return deduplicated(
                java.util.stream.Stream.concat(officialTourismResults.stream(), kakaoResults.stream())
                        .sorted(Comparator.comparingDouble(place -> distanceMeters(lat, lng, place)))
                        .toList()
        ).stream()
                .limit(NEARBY_SEARCH_RESULT_LIMIT)
                .toList();
    }

    public PlaceDetailResponse getPlaceDetail(
            String kakaoPlaceId,
            String name,
            double lat,
            double lng,
            String categoryGroupCode
    ) {
        if (!StringUtils.hasText(kakaoPlaceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카카오 장소 ID가 필요합니다.");
        }
        validateKeyword(name);
        validateCoordinate(lat, lng);

        if (isTourApiPlaceId(kakaoPlaceId)) {
            return tourismPlaceRepository.findByContentId(tourContentId(kakaoPlaceId))
                    .filter(place -> !Boolean.TRUE.equals(place.getIsDeleted()))
                    .map(place -> findKakaoTourismMatchForOfficialTourism(place)
                            .map(kakaoPlace -> matchedDetail(kakaoPlace, place))
                            .orElseGet(() -> tourApiOnlyDetail(kakaoPlaceId, place)))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "TourAPI 관광지 정보를 찾을 수 없습니다."));
        }

        KakaoPlace kakaoPlace = resolveKakaoPlace(kakaoPlaceId, name, lat, lng, categoryGroupCode)
                .map(place -> keepRequestedKakaoPlaceId(place, kakaoPlaceId))
                .orElseGet(() -> fallbackKakaoPlace(kakaoPlaceId, name, lat, lng, categoryGroupCode));
        String resolvedCategoryGroupCode = firstNonBlank(resolvedCategoryGroupCode(kakaoPlace), categoryGroupCode);

        if (isTourismCandidate(resolvedCategoryGroupCode)) {
            Optional<TourismPlace> tourismPlace = findBestTourismMatch(kakaoPlace.name(), kakaoPlace.lat(), kakaoPlace.lng());
            if (tourismPlace.isPresent()) {
                return matchedDetail(kakaoPlace, tourismPlace.get());
            }
        }

        return kakaoOnlyDetail(kakaoPlace);
    }

    private List<TourismPlace> searchOfficialTourismPlaces(String keyword, double lat, double lng, int radius) {
        Map<String, TourismPlace> places = new LinkedHashMap<>();
        tourismPlaceRepository.searchNearbyOfficialTourismPlaces(
                        keyword.trim(),
                        lat,
                        lng,
                        radius,
                        TOURISM_SEARCH_LIMIT
                )
                .forEach(place -> places.putIfAbsent(place.getContentId(), place));
        tourismPlaceRepository.searchOfficialTourismPlacesByKeyword(keyword.trim(), TOURISM_SEARCH_LIMIT)
                .forEach(place -> places.putIfAbsent(place.getContentId(), place));
        return List.copyOf(places.values());
    }

    private static List<PlaceSearchResultResponse> rankedSearchResults(
            String keyword,
            double lat,
            double lng,
            List<PlaceSearchResultResponse> kakaoResults,
            List<PlaceSearchResultResponse> officialTourismResults
    ) {
        SearchIntent intent = SearchIntent.from(keyword);
        return deduplicated(java.util.stream.Stream.concat(kakaoResults.stream(), officialTourismResults.stream())
                .sorted(Comparator.comparing((PlaceSearchResultResponse place) -> rank(keyword, intent, lat, lng, place)))
                .toList()).stream()
                .limit(PLACE_SEARCH_RESULT_LIMIT)
                .toList();
    }

    private static PlaceSearchRank rank(
            String keyword,
            SearchIntent intent,
            double lat,
            double lng,
            PlaceSearchResultResponse place
    ) {
        return new PlaceSearchRank(
                nameMatchRank(keyword, intent.searchKeywordCore(), place),
                categoryRank(intent, place),
                sourceRank(intent, place),
                distanceMeters(lat, lng, place),
                normalizeName(place.name())
        );
    }

    private static int nameMatchRank(String rawKeyword, String keywordCore, PlaceSearchResultResponse place) {
        String normalizedName = normalizeName(place.name());
        String normalizedAddress = normalizeName(place.address());
        String normalizedCategory = normalizeName(place.categoryName());
        String normalizedRawKeyword = normalizeName(rawKeyword);
        String normalizedKeyword = StringUtils.hasText(keywordCore) ? keywordCore : normalizedRawKeyword;
        boolean hasPlaceNameKeyword = StringUtils.hasText(keywordCore)
                || keywordTokens(rawKeyword).stream().anyMatch(token -> !isCategoryAlias(token));

        if (!StringUtils.hasText(normalizedKeyword) || !hasPlaceNameKeyword) {
            return 4;
        }
        if (normalizedName.equals(normalizedKeyword)) {
            return 0;
        }
        if (normalizedName.startsWith(normalizedKeyword)) {
            return 1;
        }
        if (normalizedName.contains(normalizedKeyword)) {
            return 2;
        }

        List<String> tokens = keywordTokens(rawKeyword).stream()
                .filter(token -> !isCategoryAlias(token))
                .toList();
        if (!tokens.isEmpty() && tokens.stream().allMatch(normalizedName::contains)) {
            return 3;
        }
        if (normalizedAddress.contains(normalizedKeyword)) {
            return 5;
        }
        if (normalizedCategory.contains(normalizedKeyword)) {
            return 6;
        }
        return 9;
    }

    private static int categoryRank(SearchIntent intent, PlaceSearchResultResponse place) {
        String categoryGroupCode = place.categoryGroupCode();
        if (intent.hasExplicitCategory()) {
            return intent.matches(categoryGroupCode) ? 0 : 6;
        }
        return switch (categoryGroupCode == null ? "" : categoryGroupCode) {
            case "AT4" -> 0;
            case "CE7" -> 3;
            case "FD6" -> 4;
            case "CS2" -> 5;
            case "AD5" -> 6;
            case "PK6" -> 8;
            case "BK9" -> 9;
            default -> 7;
        };
    }

    private static int sourceRank(SearchIntent intent, PlaceSearchResultResponse place) {
        if (isTourApiPlaceId(place.kakaoPlaceId()) && (!intent.hasExplicitCategory() || intent.matches("AT4"))) {
            return -1;
        }
        return 0;
    }

    private static List<PlaceSearchResultResponse> deduplicated(List<PlaceSearchResultResponse> places) {
        List<PlaceSearchResultResponse> results = new ArrayList<>();
        for (PlaceSearchResultResponse place : places) {
            boolean duplicate = results.stream().anyMatch(existing -> isLikelySamePlace(existing, place));
            if (!duplicate) {
                results.add(place);
            }
        }
        return results;
    }

    private static boolean isLikelySamePlace(PlaceSearchResultResponse left, PlaceSearchResultResponse right) {
        if (left.kakaoPlaceId().equals(right.kakaoPlaceId())) {
            return true;
        }
        if (!java.util.Objects.equals(left.categoryGroupCode(), right.categoryGroupCode())) {
            return false;
        }
        if (isTourismCandidate(left.categoryGroupCode())) {
            double nameScore = tourismClusterNameScore(left.name(), right.name());
            double distanceMeters = distanceMeters(left.lat(), left.lng(), right.lat(), right.lng());
            return isSameTourismCluster(nameScore, distanceMeters);
        }
        double nameScore = normalizedNameScore(left.name(), right.name());
        double distanceMeters = distanceMeters(left.lat(), left.lng(), right.lat(), right.lng());
        return nameScore >= MIN_TOURISM_DUPLICATE_NAME_SCORE
                && distanceMeters <= TOURISM_SEARCH_DEDUPLICATE_RADIUS_METERS;
    }

    private static List<KakaoPlace> relevantKakaoPlaces(String keyword, List<KakaoPlace> places) {
        SearchIntent intent = SearchIntent.from(keyword);
        return places.stream()
                .filter(place -> isRelevantKakaoPlace(keyword, intent, place))
                .toList();
    }

    private static boolean isTourApiPlaceId(String placeId) {
        return placeId != null && placeId.startsWith(TOUR_API_PLACE_ID_PREFIX);
    }

    private static String tourContentId(String placeId) {
        return placeId.substring(TOUR_API_PLACE_ID_PREFIX.length());
    }

    private static boolean hasDuplicateKakaoPlace(TourismPlace tourismPlace, List<KakaoPlace> kakaoPlaces) {
        return kakaoPlaces.stream()
                .anyMatch(kakaoPlace -> isLikelySamePlace(tourismPlace, kakaoPlace));
    }

    private static boolean isLikelySamePlace(TourismPlace tourismPlace, KakaoPlace kakaoPlace) {
        if (kakaoPlace.lat() == null || kakaoPlace.lng() == null) {
            return false;
        }
        if (!isTourismCandidate(resolvedCategoryGroupCode(kakaoPlace))) {
            return false;
        }
        double nameScore = tourismClusterNameScore(tourismPlace.getTitle(), kakaoPlace.name());
        double distanceMeters = distanceMeters(kakaoPlace.lat(), kakaoPlace.lng(), tourismPlace.getLocation());
        return isSameTourismCluster(nameScore, distanceMeters);
    }

    private Optional<KakaoPlace> resolveKakaoPlace(
            String kakaoPlaceId,
            String name,
            double lat,
            double lng,
            String categoryGroupCode
    ) {
        List<KakaoPlace> candidates = kakaoPlaceClient.searchKeyword(
                name,
                lat,
                lng,
                DETAIL_SEARCH_RADIUS_METERS,
                categoryGroupCode
        );
        Optional<KakaoPlace> exactPlace = findByKakaoPlaceId(candidates, kakaoPlaceId);
        if (exactPlace.isPresent()) {
            return exactPlace;
        }

        if (StringUtils.hasText(categoryGroupCode)) {
            candidates = kakaoPlaceClient.searchKeyword(name, lat, lng, DETAIL_SEARCH_RADIUS_METERS);
            exactPlace = findByKakaoPlaceId(candidates, kakaoPlaceId);
            if (exactPlace.isPresent()) {
                return exactPlace;
            }
        }

        return candidates.stream()
                .filter(place -> normalizedNameScore(place.name(), name) >= MIN_TOURISM_NAME_SCORE)
                .min(Comparator.comparingInt(place -> place.distanceMeters() == null
                        ? Integer.MAX_VALUE
                        : place.distanceMeters()));
    }

    private Optional<TourismPlace> findBestTourismMatch(String kakaoPlaceName, double lat, double lng) {
        // TODO: Improve TourAPI-Kakao name matching with aliases/tokenization if false positives appear.
        return tourismPlaceRepository.findNearbyOfficialTourismPlaces(
                        lat,
                        lng,
                        TOURISM_MATCH_RADIUS_METERS,
                        TOURISM_MATCH_LIMIT
                ).stream()
                .map(place -> new TourismMatch(
                        place,
                        tourismClusterNameScore(place.getTitle(), kakaoPlaceName),
                        distanceMeters(lat, lng, place.getLocation())
                ))
                .filter(match -> match.nameScore() >= MIN_TOURISM_NAME_SCORE)
                .sorted(Comparator.comparingDouble(TourismMatch::nameScore)
                        .reversed()
                        .thenComparingDouble(TourismMatch::distanceMeters))
                .map(TourismMatch::tourismPlace)
                .findFirst();
    }

    private Optional<KakaoPlace> findKakaoTourismMatchForOfficialTourism(TourismPlace tourismPlace) {
        List<KakaoPlace> kakaoPlaces = kakaoPlaceClient.searchKeyword(
                tourismSearchKeyword(tourismPlace.getTitle()),
                tourismPlace.getLocation().getY(),
                tourismPlace.getLocation().getX(),
                (int) TOURISM_MATCH_RADIUS_METERS
        );
        if (kakaoPlaces == null || kakaoPlaces.isEmpty()) {
            return Optional.empty();
        }

        return kakaoPlaces.stream()
                .filter(place -> isTourismCandidate(resolvedCategoryGroupCode(place)))
                .map(place -> new KakaoTourismMatch(
                        place,
                        tourismClusterNameScore(tourismPlace.getTitle(), place.name()),
                        distanceMeters(place.lat(), place.lng(), tourismPlace.getLocation())
                ))
                .filter(match -> isSameTourismCluster(match.nameScore(), match.distanceMeters()))
                .sorted(Comparator.comparingDouble(KakaoTourismMatch::nameScore)
                        .reversed()
                        .thenComparingDouble(KakaoTourismMatch::distanceMeters))
                .map(KakaoTourismMatch::kakaoPlace)
                .findFirst();
    }

    private static Optional<KakaoPlace> findByKakaoPlaceId(List<KakaoPlace> places, String kakaoPlaceId) {
        return places.stream()
                .filter(place -> kakaoPlaceId.equals(place.kakaoPlaceId()))
                .findFirst();
    }

    private static PlaceDetailResponse matchedDetail(KakaoPlace kakaoPlace, TourismPlace tourismPlace) {
        return new PlaceDetailResponse(
                kakaoPlace.kakaoPlaceId(),
                kakaoPlace.name(),
                kakaoPlace.categoryName(),
                address(kakaoPlace),
                kakaoPlace.lat(),
                kakaoPlace.lng(),
                kakaoPlace.phone(),
                kakaoPlace.placeUrl(),
                true,
                tourismPlace.getContentId(),
                tourismPlace.getContentTypeId(),
                cleanTourText(tourismPlace.getOverview()),
                tourismPlace.getFirstImageUrl(),
                cleanTourText(tourismPlace.getUseTime()),
                nullIfMissing(tourismPlace.getRawData())
        );
    }

    private static PlaceDetailResponse tourApiOnlyDetail(String kakaoPlaceId, TourismPlace tourismPlace) {
        return new PlaceDetailResponse(
                kakaoPlaceId,
                tourismPlace.getTitle(),
                officialTourismCategoryName(tourismPlace.getContentTypeId()),
                tourismAddress(tourismPlace),
                tourismPlace.getLocation().getY(),
                tourismPlace.getLocation().getX(),
                tourismPlace.getTel(),
                null,
                true,
                tourismPlace.getContentId(),
                tourismPlace.getContentTypeId(),
                cleanTourText(tourismPlace.getOverview()),
                tourismPlace.getFirstImageUrl(),
                cleanTourText(tourismPlace.getUseTime()),
                nullIfMissing(tourismPlace.getRawData())
        );
    }

    private static PlaceDetailResponse kakaoOnlyDetail(KakaoPlace kakaoPlace) {
        return new PlaceDetailResponse(
                kakaoPlace.kakaoPlaceId(),
                kakaoPlace.name(),
                kakaoPlace.categoryName(),
                address(kakaoPlace),
                kakaoPlace.lat(),
                kakaoPlace.lng(),
                kakaoPlace.phone(),
                kakaoPlace.placeUrl(),
                false,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static KakaoPlace fallbackKakaoPlace(
            String kakaoPlaceId,
            String name,
            double lat,
            double lng,
            String categoryGroupCode
    ) {
        return new KakaoPlace(
                kakaoPlaceId,
                name,
                categoryGroupCode,
                null,
                null,
                null,
                null,
                null,
                lat,
                lng,
                null,
                null
        );
    }

    private static KakaoPlace keepRequestedKakaoPlaceId(KakaoPlace place, String kakaoPlaceId) {
        if (kakaoPlaceId.equals(place.kakaoPlaceId())) {
            return place;
        }
        return new KakaoPlace(
                kakaoPlaceId,
                place.name(),
                place.categoryGroupCode(),
                place.categoryGroupName(),
                place.categoryName(),
                place.address(),
                place.roadAddress(),
                place.phone(),
                place.lat(),
                place.lng(),
                place.placeUrl(),
                place.distanceMeters()
        );
    }

    private static String resolvedCategoryGroupCode(KakaoPlace place) {
        return firstNonBlank(place.categoryGroupCode(), inferredCategoryGroupCode(place.categoryName()));
    }

    private static String inferredCategoryGroupCode(String categoryName) {
        if (!StringUtils.hasText(categoryName)) {
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

    private static boolean isTourismCandidate(String categoryGroupCode) {
        return TOURISM_CATEGORY_GROUP_CODE.equals(categoryGroupCode);
    }

    private static String address(KakaoPlace kakaoPlace) {
        return firstNonBlank(kakaoPlace.roadAddress(), kakaoPlace.address());
    }

    private static boolean isRelevantKakaoPlace(String keyword, SearchIntent intent, KakaoPlace place) {
        String normalizedKeyword = normalizeName(keyword);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return true;
        }
        if (intent.hasExplicitCategory() && intent.matches(resolvedCategoryGroupCode(place))) {
            return true;
        }

        String searchableText = normalizeName(String.join(
                " ",
                nullToEmpty(place.name()),
                nullToEmpty(place.categoryGroupName()),
                nullToEmpty(place.categoryName()),
                nullToEmpty(place.address()),
                nullToEmpty(place.roadAddress())
        ));

        if (searchableText.contains(normalizedKeyword)) {
            return true;
        }

        return keywordTokens(keyword).stream()
                .allMatch(token -> searchableText.contains(token) || categoryAliasMatches(token, place));
    }

    private static List<String> keywordTokens(String keyword) {
        return java.util.Arrays.stream(keyword.trim().split("\\s+"))
                .map(PlaceService::normalizeName)
                .filter(StringUtils::hasText)
                .filter(token -> !List.of("제주", "제주도", "제주특별자치도").contains(token))
                .toList();
    }

    private static boolean categoryAliasMatches(String token, KakaoPlace place) {
        String categoryText = normalizeName(String.join(
                " ",
                nullToEmpty(place.categoryGroupName()),
                nullToEmpty(place.categoryName())
        ));
        return switch (categoryGroupForAlias(token)) {
            case "FD6" -> categoryText.contains("음식점");
            case "CE7" -> categoryText.contains("카페");
            case "CS2" -> categoryText.contains("편의점");
            case "AD5" -> categoryText.contains("숙박");
            case "AT4" -> categoryText.contains("관광") || categoryText.contains("명소") || categoryText.contains("여행");
            default -> false;
        };
    }

    private static String normalizeCategoryGroupCode(String categoryGroupCode) {
        if (!StringUtils.hasText(categoryGroupCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카테고리 코드가 필요합니다.");
        }
        String normalized = categoryGroupCode.trim().toUpperCase();
        return switch (normalized) {
            case "AT4", "CE7", "FD6", "CS2", "AD5" -> normalized;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 장소 카테고리입니다.");
        };
    }

    private static String categoryKeyword(String categoryGroupCode) {
        return switch (categoryGroupCode) {
            case "AT4" -> "관광지";
            case "CE7" -> "카페";
            case "FD6" -> "맛집";
            case "CS2" -> "편의점";
            case "AD5" -> "숙소";
            default -> "장소";
        };
    }

    private static boolean isCategoryAlias(String token) {
        return StringUtils.hasText(categoryGroupForAlias(token));
    }

    private static String categoryGroupForAlias(String token) {
        return switch (normalizeName(token)) {
            case "맛집", "식당", "음식점", "밥집", "고기", "해산물" -> "FD6";
            case "카페", "커피", "디저트", "베이커리" -> "CE7";
            case "편의점", "마트", "상점" -> "CS2";
            case "숙소", "숙박", "호텔", "펜션", "게스트하우스", "리조트" -> "AD5";
            case "관광", "관광지", "명소", "여행", "오름", "해변", "숲길", "전망대" -> "AT4";
            default -> "";
        };
    }

    private static String cleanTourText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("(?i)</p\\s*>", "\n");
        text = text.replaceAll("<[^>]+>", " ");
        text = HtmlUtils.htmlUnescape(text);
        text = text.replace('\u00a0', ' ');
        text = text.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        text = text.replaceAll("\\n\\s*", "\n");
        text = text.replaceAll("\\n{3,}", "\n\n");
        text = text.trim();
        return text.isBlank() ? null : text;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String officialTourismCategoryName(String contentTypeId) {
        return switch (contentTypeId) {
            case "14" -> "문화시설";
            case "28" -> "레포츠";
            default -> "관광지";
        };
    }

    private static String tourismAddress(TourismPlace place) {
        if (!StringUtils.hasText(place.getAddress())) {
            return place.getDetailAddress();
        }
        if (!StringUtils.hasText(place.getDetailAddress())) {
            return place.getAddress();
        }
        return place.getAddress() + " " + place.getDetailAddress();
    }

    private static String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private static JsonNode nullIfMissing(JsonNode jsonNode) {
        return jsonNode == null || jsonNode.isMissingNode() || jsonNode.isNull() ? null : jsonNode;
    }

    private static void validateKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어를 입력해주세요.");
        }
    }

    private static void validateCoordinate(double lat, double lng) {
        if (Double.isNaN(lat) || Double.isNaN(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 좌표가 필요합니다.");
        }
    }

    private static int normalizeRadius(Integer radiusMeters) {
        if (radiusMeters == null) {
            return DEFAULT_SEARCH_RADIUS_METERS;
        }
        if (radiusMeters <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색 반경은 1m 이상이어야 합니다.");
        }
        return Math.min(radiusMeters, MAX_SEARCH_RADIUS_METERS);
    }

    private static double normalizedNameScore(String left, String right) {
        String normalizedLeft = normalizeName(left);
        String normalizedRight = normalizeName(right);
        if (!StringUtils.hasText(normalizedLeft) || !StringUtils.hasText(normalizedRight)) {
            return 0.0;
        }
        if (normalizedLeft.equals(normalizedRight)) {
            return 1.0;
        }
        if (normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft)) {
            return 0.85;
        }

        long commonCharacterCount = normalizedLeft.chars()
                .filter(character -> normalizedRight.indexOf(character) >= 0)
                .count();
        return commonCharacterCount / (double) Math.max(normalizedLeft.length(), normalizedRight.length());
    }

    private static double tourismClusterNameScore(String left, String right) {
        String canonicalLeft = canonicalTourismName(left);
        String canonicalRight = canonicalTourismName(right);
        if (!StringUtils.hasText(canonicalLeft) || !StringUtils.hasText(canonicalRight)) {
            return normalizedNameScore(left, right);
        }
        if (canonicalLeft.equals(canonicalRight)) {
            return 1.0;
        }
        if (canonicalLeft.contains(canonicalRight) || canonicalRight.contains(canonicalLeft)) {
            return 0.9;
        }
        return normalizedNameScore(canonicalLeft, canonicalRight);
    }

    private static boolean isSameTourismCluster(double nameScore, double distanceMeters) {
        if (nameScore >= 1.0 && distanceMeters <= TOURISM_CLUSTER_DEDUPLICATE_RADIUS_METERS) {
            return true;
        }
        return nameScore >= MIN_TOURISM_CLUSTER_NAME_SCORE
                && distanceMeters <= TOURISM_SEARCH_DEDUPLICATE_RADIUS_METERS;
    }

    private static String canonicalTourismName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String canonicalName = value
                .replaceAll("\\[[^\\]]+]", " ")
                .replaceAll("\\([^)]{2,}\\)", " ");
        canonicalName = normalizeName(canonicalName);

        List<String> routeSuffixes = List.of("정상전망대", "전망대", "관광지");
        List<String> areaSuffixes = List.of("해양도립공원", "도립공원", "국립공원");
        boolean changed;
        do {
            changed = false;
            for (String suffix : routeSuffixes) {
                String normalizedSuffix = normalizeName(suffix);
                if (canonicalName.endsWith(normalizedSuffix)
                        && canonicalName.length() > normalizedSuffix.length() + 1) {
                    canonicalName = canonicalName.substring(0, canonicalName.length() - normalizedSuffix.length());
                    changed = true;
                }
            }
            for (String suffix : areaSuffixes) {
                String normalizedSuffix = normalizeName(suffix);
                if (!canonicalName.endsWith(normalizedSuffix)) {
                    continue;
                }
                String stem = canonicalName.substring(0, canonicalName.length() - normalizedSuffix.length());
                if (hasDistinctTourismLandmarkSuffix(stem)) {
                    canonicalName = stem;
                    changed = true;
                }
            }
        } while (changed);

        return canonicalName;
    }

    private static String tourismSearchKeyword(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String keyword = value
                .replaceAll("\\[[^\\]]+]", " ")
                .replaceAll("\\([^)]{2,}\\)", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return keyword.isBlank() ? value.trim() : keyword;
    }

    private static boolean hasDistinctTourismLandmarkSuffix(String value) {
        return value.endsWith("봉")
                || value.endsWith("산")
                || value.endsWith("오름")
                || value.endsWith("도");
    }

    private static String normalizeName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replaceAll("[^0-9A-Za-z가-힣]", "").toLowerCase();
    }

    private static double distanceMeters(double lat, double lng, Point point) {
        return distanceMeters(lat, lng, point.getY(), point.getX());
    }

    private static double distanceMeters(double lat, double lng, PlaceSearchResultResponse place) {
        return distanceMeters(lat, lng, place.lat(), place.lng());
    }

    private static double distanceMeters(double lat, double lng, double targetLat, double targetLng) {
        double latRadians = Math.toRadians(targetLat - lat);
        double lngRadians = Math.toRadians(targetLng - lng);
        double haversine = Math.sin(latRadians / 2) * Math.sin(latRadians / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(targetLat))
                * Math.sin(lngRadians / 2) * Math.sin(lngRadians / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private record TourismMatch(
            TourismPlace tourismPlace,
            double nameScore,
            double distanceMeters
    ) {
    }

    private record KakaoTourismMatch(
            KakaoPlace kakaoPlace,
            double nameScore,
            double distanceMeters
    ) {
    }

    private record SearchIntent(
            List<String> categoryGroupCodes,
            boolean hasExplicitCategory,
            String searchKeywordCore
    ) {

        private static SearchIntent from(String keyword) {
            String normalizedKeyword = normalizeName(keyword)
                    .replace("제주특별자치도", "")
                    .replace("제주도", "")
                    .replace("제주", "");
            List<String> categoryGroupCodes = new ArrayList<>();
            String searchKeywordCore = normalizedKeyword;

            for (String alias : CATEGORY_ALIASES) {
                String normalizedAlias = normalizeName(alias);
                if (normalizedKeyword.contains(normalizedAlias)) {
                    String categoryGroupCode = categoryGroupForAlias(normalizedAlias);
                    if (StringUtils.hasText(categoryGroupCode) && !categoryGroupCodes.contains(categoryGroupCode)) {
                        categoryGroupCodes.add(categoryGroupCode);
                    }
                    searchKeywordCore = searchKeywordCore.replace(normalizedAlias, "");
                }
            }

            return new SearchIntent(
                    List.copyOf(categoryGroupCodes),
                    !categoryGroupCodes.isEmpty(),
                    searchKeywordCore
            );
        }

        private boolean matches(String categoryGroupCode) {
            return StringUtils.hasText(categoryGroupCode) && categoryGroupCodes.contains(categoryGroupCode);
        }
    }

    private record PlaceSearchRank(
            int nameMatchRank,
            int categoryRank,
            int sourceRank,
            double distanceMeters,
            String normalizedName
    ) implements Comparable<PlaceSearchRank> {

        @Override
        public int compareTo(PlaceSearchRank other) {
            int compared = Integer.compare(nameMatchRank, other.nameMatchRank);
            if (compared != 0) {
                return compared;
            }
            compared = Integer.compare(categoryRank, other.categoryRank);
            if (compared != 0) {
                return compared;
            }
            compared = Integer.compare(sourceRank, other.sourceRank);
            if (compared != 0) {
                return compared;
            }
            compared = Double.compare(distanceMeters, other.distanceMeters);
            if (compared != 0) {
                return compared;
            }
            return normalizedName.compareTo(other.normalizedName);
        }
    }
}
