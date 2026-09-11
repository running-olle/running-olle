package com.runningolle.domain.tourism.client;

import com.runningolle.global.client.ExternalApiRestClientSupport;
import com.runningolle.global.config.properties.ExternalApiProperties;
import com.runningolle.global.exception.ExternalApiException;
import java.net.URLDecoder;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TourApiClient {

    private static final String PROVIDER = "TourAPI";
    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2";
    private static final String JSON_TYPE = "json";
    private static final String TOURISM_CONTENT_TYPE_ID = "12";
    private static final int MAX_LOCATION_RADIUS_METERS = 20_000;
    private static final DateTimeFormatter TOUR_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final ExternalApiProperties properties;
    private final RestClient restClient;

    public TourApiClient(ExternalApiProperties properties) {
        this.properties = properties;
        this.restClient = ExternalApiRestClientSupport.restClient(BASE_URL);
    }

    public List<TourLocationItem> findNearbyTourism(double lat, double lng, int radiusMeters) {
        validateApiKey();

        try {
            Map<String, Object> response = restClient.get()
                    .uri(tourApiUri("/locationBasedList2", Map.ofEntries(
                            Map.entry("serviceKey", serviceKey()),
                            Map.entry("MobileOS", properties.getTourMobileOs()),
                            Map.entry("MobileApp", properties.getTourMobileApp()),
                            Map.entry("_type", JSON_TYPE),
                            Map.entry("numOfRows", 10),
                            Map.entry("pageNo", 1),
                            Map.entry("arrange", "E"),
                            Map.entry("contentTypeId", TOURISM_CONTENT_TYPE_ID),
                            Map.entry("mapX", lng),
                            Map.entry("mapY", lat),
                            Map.entry("radius", clampRadius(radiusMeters))
                    )))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, ExternalApiRestClientSupport.errorHandler(PROVIDER))
                    .body(new ParameterizedTypeReference<>() {
                    });

            validateTourApiResponse(response, "위치 기반 관광정보 조회");
            return extractItems(response).stream()
                    .map(TourLocationItem::from)
                    .toList();
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalApiException(PROVIDER, "TourAPI 위치 기반 관광정보 조회에 실패했습니다.", exception);
        }
    }

    public TourAreaPage getAreaBasedList(String areaCode, String contentTypeId, int pageNo, int numOfRows) {
        validateApiKey();

        try {
            Map<String, Object> response = restClient.get()
                    .uri(tourApiUri("/areaBasedList2", Map.ofEntries(
                            Map.entry("serviceKey", serviceKey()),
                            Map.entry("MobileOS", properties.getTourMobileOs()),
                            Map.entry("MobileApp", properties.getTourMobileApp()),
                            Map.entry("_type", JSON_TYPE),
                            Map.entry("numOfRows", Math.max(1, numOfRows)),
                            Map.entry("pageNo", Math.max(1, pageNo)),
                            Map.entry("arrange", "C"),
                            Map.entry("areaCode", areaCode),
                            Map.entry("contentTypeId", contentTypeId)
                    )))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, ExternalApiRestClientSupport.errorHandler(PROVIDER))
                    .body(new ParameterizedTypeReference<>() {
                    });

            validateTourApiResponse(response, "지역 기반 관광정보 조회");
            return new TourAreaPage(
                    extractItems(response).stream()
                            .map(TourAreaItem::from)
                            .toList(),
                    Math.max(1, pageNo),
                    Math.max(1, numOfRows),
                    extractTotalCount(response)
            );
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalApiException(PROVIDER, "TourAPI 지역 기반 관광정보 조회에 실패했습니다.", exception);
        }
    }

    public TourFestivalPage searchFestival(
            String areaCode,
            LocalDate eventStartDate,
            LocalDate eventEndDate,
            int pageNo,
            int numOfRows
    ) {
        validateApiKey();

        try {
            Map<String, Object> queryParams = new LinkedHashMap<>();
            queryParams.put("serviceKey", serviceKey());
            queryParams.put("MobileOS", properties.getTourMobileOs());
            queryParams.put("MobileApp", properties.getTourMobileApp());
            queryParams.put("_type", JSON_TYPE);
            queryParams.put("numOfRows", Math.max(1, numOfRows));
            queryParams.put("pageNo", Math.max(1, pageNo));
            queryParams.put("arrange", "A");
            queryParams.put("areaCode", areaCode);
            queryParams.put("eventStartDate", TOUR_DATE_FORMATTER.format(eventStartDate));
            if (eventEndDate != null) {
                queryParams.put("eventEndDate", TOUR_DATE_FORMATTER.format(eventEndDate));
            }

            Map<String, Object> response = restClient.get()
                    .uri(tourApiUri("/searchFestival2", queryParams))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, ExternalApiRestClientSupport.errorHandler(PROVIDER))
                    .body(new ParameterizedTypeReference<>() {
                    });

            validateTourApiResponse(response, "행사정보 조회");
            return new TourFestivalPage(
                    extractItems(response).stream()
                            .map(TourFestivalItem::from)
                            .toList(),
                    Math.max(1, pageNo),
                    Math.max(1, numOfRows),
                    extractTotalCount(response)
            );
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalApiException(PROVIDER, "TourAPI 행사정보 조회에 실패했습니다.", exception);
        }
    }

    public Optional<TourCommonDetail> getCommonDetail(String contentId) {
        if (!StringUtils.hasText(contentId)) {
            return Optional.empty();
        }
        validateApiKey();

        try {
            Map<String, Object> response = restClient.get()
                    .uri(tourApiUri("/detailCommon2", Map.ofEntries(
                            Map.entry("serviceKey", serviceKey()),
                            Map.entry("MobileOS", properties.getTourMobileOs()),
                            Map.entry("MobileApp", properties.getTourMobileApp()),
                            Map.entry("_type", JSON_TYPE),
                            Map.entry("contentId", contentId.trim()),
                            Map.entry("numOfRows", 1),
                            Map.entry("pageNo", 1)
                    )))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, ExternalApiRestClientSupport.errorHandler(PROVIDER))
                    .body(new ParameterizedTypeReference<>() {
                    });

            validateTourApiResponse(response, "공통정보 조회");
            return extractFirstItem(response).map(TourCommonDetail::from);
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalApiException(PROVIDER, "TourAPI 공통정보 조회에 실패했습니다.", exception);
        }
    }

    public Optional<TourIntroDetail> getIntroDetail(String contentId, String contentTypeId) {
        if (!StringUtils.hasText(contentId) || !StringUtils.hasText(contentTypeId)) {
            return Optional.empty();
        }
        validateApiKey();

        try {
            Map<String, Object> response = restClient.get()
                    .uri(tourApiUri("/detailIntro2", Map.of(
                            "serviceKey", serviceKey(),
                            "MobileOS", properties.getTourMobileOs(),
                            "MobileApp", properties.getTourMobileApp(),
                            "_type", JSON_TYPE,
                            "contentId", contentId.trim(),
                            "contentTypeId", contentTypeId.trim(),
                            "numOfRows", 1,
                            "pageNo", 1
                    )))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, ExternalApiRestClientSupport.errorHandler(PROVIDER))
                    .body(new ParameterizedTypeReference<>() {
                    });

            validateTourApiResponse(response, "소개정보 조회");
            return extractFirstItem(response).map(TourIntroDetail::from);
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalApiException(PROVIDER, "TourAPI 소개정보 조회에 실패했습니다.", exception);
        }
    }

    public Optional<TourDetail> getDetail(String contentId, String contentTypeId) {
        Optional<TourCommonDetail> commonDetail = getCommonDetail(contentId);
        Optional<TourIntroDetail> introDetail = getIntroDetail(contentId, contentTypeId);

        if (commonDetail.isEmpty() && introDetail.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> raw = new LinkedHashMap<>();
        commonDetail.ifPresent(detail -> raw.put("detailCommon2", detail.raw()));
        introDetail.ifPresent(detail -> raw.put("detailIntro2", detail.raw()));

        TourCommonDetail common = commonDetail.orElse(null);
        TourIntroDetail intro = introDetail.orElse(null);

        return Optional.of(new TourDetail(
                common == null ? contentId : common.contentId(),
                common == null ? contentTypeId : common.contentTypeId(),
                common == null ? null : common.title(),
                common == null ? null : common.address(),
                common == null ? null : common.detailAddress(),
                common == null ? null : common.areaCode(),
                common == null ? null : common.sigunguCode(),
                common == null ? null : common.category1(),
                common == null ? null : common.category2(),
                common == null ? null : common.category3(),
                common == null ? null : common.lat(),
                common == null ? null : common.lng(),
                common == null ? null : common.overview(),
                common == null ? null : common.firstImageUrl(),
                intro == null ? null : intro.useTime(),
                raw
        ));
    }

    private void validateApiKey() {
        if (!StringUtils.hasText(properties.getTourApiKey())) {
            throw new ExternalApiException(PROVIDER, "TourAPI 서비스 키가 설정되지 않았습니다.");
        }
    }

    private String serviceKey() {
        String key = properties.getTourApiKey().trim();
        if (key.contains("%")) {
            return URLDecoder.decode(key, StandardCharsets.UTF_8);
        }
        return key;
    }

    private URI tourApiUri(String path, Map<String, ?> queryParams) {
        StringBuilder query = new StringBuilder(BASE_URL).append(path).append('?');
        boolean first = true;
        for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
            if (!first) {
                query.append('&');
            }
            first = false;
            query.append(urlEncode(entry.getKey()))
                    .append('=')
                    .append(urlEncode(String.valueOf(entry.getValue())));
        }
        return URI.create(query.toString());
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static int clampRadius(int radiusMeters) {
        return Math.max(1, Math.min(radiusMeters, MAX_LOCATION_RADIUS_METERS));
    }

    private static void validateTourApiResponse(Map<String, Object> response, String operation) {
        if (response == null) {
            throw new ExternalApiException(PROVIDER, operation + " 응답이 비어 있습니다.");
        }

        Object openApiError = getNestedValue(
                response,
                "OpenAPI_ServiceResponse",
                "cmmMsgHeader",
                "returnAuthMsg"
        );
        if (openApiError != null) {
            throw new ExternalApiException(PROVIDER, operation + " 실패: " + openApiError);
        }

        Object directResultCode = response.get("resultCode");
        if (directResultCode != null && !"0000".equals(String.valueOf(directResultCode))) {
            Object directResultMessage = response.get("resultMsg");
            throw new ExternalApiException(
                    PROVIDER,
                    operation + " 실패: " + (directResultMessage == null ? directResultCode : directResultMessage)
            );
        }

        Object resultCode = getNestedValue(response, "response", "header", "resultCode");
        if ("0000".equals(String.valueOf(resultCode))) {
            return;
        }

        if (resultCode == null) {
            throw new ExternalApiException(PROVIDER, operation + " 응답 형식이 올바르지 않습니다.");
        }

        Object resultMessage = getNestedValue(response, "response", "header", "resultMsg");
        throw new ExternalApiException(
                PROVIDER,
                operation + " 실패: " + (resultMessage == null ? resultCode : resultMessage)
        );
    }

    private static Optional<Map<String, Object>> extractFirstItem(Map<String, Object> response) {
        return extractItems(response).stream().findFirst();
    }

    private static List<Map<String, Object>> extractItems(Map<String, Object> response) {
        Object item = getNestedValue(response, "response", "body", "items", "item");

        if (item instanceof List<?> items) {
            return items.stream()
                    .filter(Map.class::isInstance)
                    .map(TourApiClient::toStringObjectMap)
                    .toList();
        }

        if (item instanceof Map<?, ?>) {
            return List.of(toStringObjectMap(item));
        }

        return List.of();
    }

    private static int extractTotalCount(Map<String, Object> response) {
        Integer totalCount = readInteger(getNestedValue(response, "response", "body", "totalCount"));
        return totalCount == null ? 0 : totalCount;
    }

    private static Object getNestedValue(Map<String, Object> source, String... keys) {
        Object current = source;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toStringObjectMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static String readString(Map<String, Object> raw, String key) {
        Object value = raw.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private static Double readDouble(Map<String, Object> raw, String key) {
        String value = readString(raw, key);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer readInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String readFirstString(Map<String, Object> raw, String... keys) {
        for (String key : keys) {
            String value = readString(raw, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public record TourLocationItem(
            String contentId,
            String contentTypeId,
            String title,
            String address,
            Double lat,
            Double lng,
            String firstImageUrl,
            Map<String, Object> raw
    ) {

        private static TourLocationItem from(Map<String, Object> raw) {
            return new TourLocationItem(
                    readString(raw, "contentid"),
                    readString(raw, "contenttypeid"),
                    readString(raw, "title"),
                    readString(raw, "addr1"),
                    readDouble(raw, "mapy"),
                    readDouble(raw, "mapx"),
                    readString(raw, "firstimage"),
                    raw
            );
        }
    }

    public record TourAreaPage(
            List<TourAreaItem> items,
            int pageNo,
            int numOfRows,
            int totalCount
    ) {
    }

    public record TourFestivalPage(
            List<TourFestivalItem> items,
            int pageNo,
            int numOfRows,
            int totalCount
    ) {
    }

    public record TourFestivalItem(
            String contentId,
            String contentTypeId,
            String title,
            String address,
            String detailAddress,
            String areaCode,
            String sigunguCode,
            String category1,
            String category2,
            String category3,
            String tel,
            Double lat,
            Double lng,
            String firstImageUrl,
            String thumbnailImageUrl,
            String eventStartDate,
            String eventEndDate,
            String createdTime,
            String modifiedTime,
            Map<String, Object> raw
    ) {

        private static TourFestivalItem from(Map<String, Object> raw) {
            return new TourFestivalItem(
                    readString(raw, "contentid"),
                    readString(raw, "contenttypeid"),
                    readString(raw, "title"),
                    readString(raw, "addr1"),
                    readString(raw, "addr2"),
                    readString(raw, "areacode"),
                    readString(raw, "sigungucode"),
                    readString(raw, "cat1"),
                    readString(raw, "cat2"),
                    readString(raw, "cat3"),
                    readString(raw, "tel"),
                    readDouble(raw, "mapy"),
                    readDouble(raw, "mapx"),
                    readString(raw, "firstimage"),
                    readFirstString(raw, "firstimage2", "firstimage"),
                    readString(raw, "eventstartdate"),
                    readString(raw, "eventenddate"),
                    readString(raw, "createdtime"),
                    readString(raw, "modifiedtime"),
                    raw
            );
        }
    }

    public record TourAreaItem(
            String contentId,
            String contentTypeId,
            String title,
            String address,
            String detailAddress,
            String areaCode,
            String sigunguCode,
            String category1,
            String category2,
            String category3,
            String tel,
            Double lat,
            Double lng,
            String firstImageUrl,
            String thumbnailImageUrl,
            String createdTime,
            String modifiedTime,
            Map<String, Object> raw
    ) {

        private static TourAreaItem from(Map<String, Object> raw) {
            return new TourAreaItem(
                    readString(raw, "contentid"),
                    readString(raw, "contenttypeid"),
                    readString(raw, "title"),
                    readString(raw, "addr1"),
                    readString(raw, "addr2"),
                    readString(raw, "areacode"),
                    readString(raw, "sigungucode"),
                    readString(raw, "cat1"),
                    readString(raw, "cat2"),
                    readString(raw, "cat3"),
                    readString(raw, "tel"),
                    readDouble(raw, "mapy"),
                    readDouble(raw, "mapx"),
                    readString(raw, "firstimage"),
                    readFirstString(raw, "firstimage2", "firstimage"),
                    readString(raw, "createdtime"),
                    readString(raw, "modifiedtime"),
                    raw
            );
        }
    }

    public record TourCommonDetail(
            String contentId,
            String contentTypeId,
            String title,
            String address,
            String detailAddress,
            String areaCode,
            String sigunguCode,
            String category1,
            String category2,
            String category3,
            Double lat,
            Double lng,
            String overview,
            String firstImageUrl,
            Map<String, Object> raw
    ) {

        private static TourCommonDetail from(Map<String, Object> raw) {
            return new TourCommonDetail(
                    readString(raw, "contentid"),
                    readString(raw, "contenttypeid"),
                    readString(raw, "title"),
                    readString(raw, "addr1"),
                    readString(raw, "addr2"),
                    readString(raw, "areacode"),
                    readString(raw, "sigungucode"),
                    readString(raw, "cat1"),
                    readString(raw, "cat2"),
                    readString(raw, "cat3"),
                    readDouble(raw, "mapy"),
                    readDouble(raw, "mapx"),
                    readString(raw, "overview"),
                    readString(raw, "firstimage"),
                    raw
            );
        }
    }

    public record TourIntroDetail(
            String contentId,
            String contentTypeId,
            String useTime,
            Map<String, Object> raw
    ) {

        private static TourIntroDetail from(Map<String, Object> raw) {
            return new TourIntroDetail(
                    readString(raw, "contentid"),
                    readString(raw, "contenttypeid"),
                    readFirstString(
                            raw,
                            "usetime",
                            "usetimeculture",
                            "usetimefestival",
                            "usetimeleports",
                            "opentimefood",
                            "opentime"
                    ),
                    raw
            );
        }
    }

    public record TourDetail(
            String contentId,
            String contentTypeId,
            String title,
            String address,
            String detailAddress,
            String areaCode,
            String sigunguCode,
            String category1,
            String category2,
            String category3,
            Double lat,
            Double lng,
            String overview,
            String firstImageUrl,
            String useTime,
            Map<String, Object> raw
    ) {
    }
}
