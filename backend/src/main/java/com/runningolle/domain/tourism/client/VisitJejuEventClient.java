package com.runningolle.domain.tourism.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.global.client.ExternalApiRestClientSupport;
import com.runningolle.global.exception.ExternalApiException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class VisitJejuEventClient {

    private static final String PROVIDER = "VisitJeju";
    private static final String BASE_URL = "https://api.visitjeju.net";
    private static final String SITE_ID = "jejuavj";
    private static final String LOCALE = "kr";
    private static final String DEVICE = "pc";
    private static final String FESTIVAL_CONTENTS_CODE = "c5";
    private static final String EVENT_STATE_ALL = "all";
    private static final String SORTING = "likecnt desc";
    private static final String SECTIGO_ROOT_R46_CERTIFICATE =
            "certs/sectigo-public-server-authentication-root-r46.pem";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private String cookieHeader;

    public VisitJejuEventClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = ExternalApiRestClientSupport.restClient(BASE_URL, visitJejuSslContext());
    }

    public VisitJejuEventPage getFestivalEvents(int year, String month, int pageNo, int pageSize) {
        ensureToken();
        try {
            Map<String, Object> response = requestFestivalEvents(year, month, pageNo, pageSize);
            if (isUnauthorized(response)) {
                refreshToken();
                response = requestFestivalEvents(year, month, pageNo, pageSize);
            }
            validateResponse(response);
            return new VisitJejuEventPage(
                    extractItems(response).stream()
                            .map(this::toItem)
                            .flatMap(Optional::stream)
                            .toList(),
                    intValue(response.get("currentPage"), pageNo),
                    intValue(response.get("pageSize"), pageSize),
                    intValue(response.get("totalCount"), 0)
            );
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalApiException(PROVIDER, "비짓제주 행사 목록 조회에 실패했습니다.", exception);
        }
    }

    private Map<String, Object> requestFestivalEvents(int year, String month, int pageNo, int pageSize) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/contents/list")
                        .queryParam("_siteId", SITE_ID)
                        .queryParam("locale", LOCALE)
                        .queryParam("device", DEVICE)
                        .queryParam("sorting", SORTING)
                        .queryParam("year", year)
                        .queryParam("month", month)
                        .queryParam("festivalcontents", "y")
                        .queryParam("contentscd", FESTIVAL_CONTENTS_CODE)
                        .queryParam("pageSize", Math.max(1, pageSize))
                        .queryParam("page", Math.max(1, pageNo))
                        .queryParam("state", EVENT_STATE_ALL)
                        .build())
                .header(HttpHeaders.COOKIE, cookieHeader)
                .retrieve()
                .onStatus(status -> status.value() == 401, (request, response) -> {
                })
                .onStatus(
                        status -> status.isError() && status.value() != 401,
                        ExternalApiRestClientSupport.errorHandler(PROVIDER)
                )
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private synchronized void ensureToken() {
        if (StringUtils.hasText(cookieHeader)) {
            return;
        }
        refreshToken();
    }

    private synchronized void refreshToken() {
        try {
            ResponseEntity<Map<String, Object>> response = restClient.get()
                    .uri("/api/auth/token")
                    .retrieve()
                    .onStatus(HttpStatusCode -> HttpStatusCode.isError(), ExternalApiRestClientSupport.errorHandler(PROVIDER))
                    .toEntity(new ParameterizedTypeReference<>() {
                    });

            cookieHeader = toCookieHeader(response.getHeaders().get(HttpHeaders.SET_COOKIE));
            if (!StringUtils.hasText(cookieHeader)) {
                throw new ExternalApiException(PROVIDER, "비짓제주 익명 토큰 쿠키를 받지 못했습니다.");
            }
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalApiException(PROVIDER, "비짓제주 익명 토큰 발급에 실패했습니다.", exception);
        }
    }

    private SSLContext visitJejuSslContext() {
        try {
            X509TrustManager defaultTrustManager = trustManager(null);
            X509TrustManager visitJejuTrustManager = trustManager(visitJejuTrustStore());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{
                    new CompositeX509TrustManager(defaultTrustManager, visitJejuTrustManager)
            }, null);
            return sslContext;
        } catch (Exception exception) {
            throw new IllegalStateException("비짓제주 SSL 설정을 초기화하지 못했습니다.", exception);
        }
    }

    private KeyStore visitJejuTrustStore() throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Certificate certificate;
        try (InputStream inputStream = new ClassPathResource(SECTIGO_ROOT_R46_CERTIFICATE).getInputStream()) {
            certificate = certificateFactory.generateCertificate(inputStream);
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        keyStore.setCertificateEntry("sectigo-public-server-authentication-root-r46", certificate);
        return keyStore;
    }

    private X509TrustManager trustManager(KeyStore keyStore) throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
        );
        trustManagerFactory.init(keyStore);

        return Arrays.stream(trustManagerFactory.getTrustManagers())
                .filter(X509TrustManager.class::isInstance)
                .map(X509TrustManager.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("X509TrustManager를 찾지 못했습니다."));
    }

    private String toCookieHeader(List<String> setCookies) {
        if (setCookies == null || setCookies.isEmpty()) {
            return null;
        }
        return setCookies.stream()
                .map(cookie -> cookie.split(";", 2)[0])
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + "; " + right)
                .orElse(null);
    }

    private Optional<VisitJejuEventItem> toItem(Map<String, Object> raw) {
        String contentId = stringValue(raw.get("contentsid"));
        String title = cleanText(stringValue(raw.get("title")));
        LocalDate startDate = parseDate(firstFestivalValue(raw, "stday"));
        LocalDate endDate = parseDate(firstNonBlank(firstFestivalValue(raw, "fnsday"), firstFestivalValue(raw, "stday")));

        if (!StringUtils.hasText(contentId)
                || !StringUtils.hasText(title)
                || startDate == null
                || endDate == null) {
            return Optional.empty();
        }

        return Optional.of(new VisitJejuEventItem(
                contentId,
                title,
                cleanText(firstNonBlank(stringValue(raw.get("roadaddress")), stringValue(raw.get("address")))),
                cleanText(stringValue(raw.get("address"))),
                cleanText(firstNonBlank(firstFestivalValue(raw, "host"), stringValue(raw.get("mgmtdept")))),
                cleanText(stringValue(raw.get("phoneno"))),
                cleanText(firstFestivalValue(raw, "spv")),
                startDate,
                endDate,
                categoryValue(raw, "cate2cd", "value"),
                categoryValue(raw, "cate2cd", "label"),
                doubleValue(raw.get("latitude")),
                doubleValue(raw.get("longitude")),
                imageValue(raw, "imgpath"),
                imageValue(raw, "thumbnailpath"),
                cleanText(firstNonBlank(stringValue(raw.get("introduction")), stringValue(raw.get("sbstseo")))),
                cleanText(firstNonBlank(stringValue(raw.get("tag")), stringValue(raw.get("alltag")))),
                stringValue(raw.get("created")),
                stringValue(raw.get("changed")),
                sourceUrl(contentId),
                objectMapper.valueToTree(raw)
        ));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractItems(Map<String, Object> response) {
        Object items = response == null ? null : response.get("items");
        if (!(items instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private static void validateResponse(Map<String, Object> response) {
        if (response == null) {
            throw new ExternalApiException(PROVIDER, "비짓제주 행사 목록 응답이 비어 있습니다.");
        }
        String result = stringValue(response.get("result"));
        if (!"200".equals(result)) {
            throw new ExternalApiException(PROVIDER, "비짓제주 행사 목록 조회에 실패했습니다. result=" + result);
        }
    }

    private static boolean isUnauthorized(Map<String, Object> response) {
        return response != null
                && ("UNAUTHORIZED".equals(stringValue(response.get("status")))
                || "AUTHENTICATION".equals(stringValue(response.get("code"))));
    }

    @SuppressWarnings("unchecked")
    private static String firstFestivalValue(Map<String, Object> raw, String key) {
        Object festivalContents = raw.get("festivalcontents");
        if (!(festivalContents instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (first instanceof Map<?, ?> map) {
            return stringValue(((Map<String, Object>) map).get(key));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String categoryValue(Map<String, Object> raw, String categoryKey, String valueKey) {
        Object categoryMappings = raw.get("catemappList");
        if (!(categoryMappings instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> map)) {
            return null;
        }
        Object category = ((Map<String, Object>) map).get(categoryKey);
        if (category instanceof Map<?, ?> categoryMap) {
            return stringValue(((Map<String, Object>) categoryMap).get(valueKey));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String imageValue(Map<String, Object> raw, String key) {
        Object repPhoto = raw.get("repPhoto");
        if (!(repPhoto instanceof Map<?, ?> repPhotoMap)) {
            return null;
        }
        Object photoId = ((Map<String, Object>) repPhotoMap).get("photoid");
        if (photoId instanceof Map<?, ?> photoMap) {
            return stringValue(((Map<String, Object>) photoMap).get(key));
        }
        return null;
    }

    private static String sourceUrl(String contentId) {
        return "https://www.visitjeju.net/kr/festival/view?contentsid=" + contentId;
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return StringUtils.hasText(String.valueOf(value)) ? Integer.parseInt(String.valueOf(value)) : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static Double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            String text = stringValue(value);
            return text == null ? null : Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String cleanText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value
                .replace("<br>", "\n")
                .replace("<br/>", "\n")
                .replace("<br />", "\n")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    public record VisitJejuEventPage(
            List<VisitJejuEventItem> items,
            int pageNo,
            int pageSize,
            int totalCount
    ) {
    }

    public record VisitJejuEventItem(
            String contentId,
            String title,
            String roadAddress,
            String address,
            String venueName,
            String tel,
            String organizer,
            LocalDate eventStartDate,
            LocalDate eventEndDate,
            String categoryCode,
            String categoryLabel,
            Double lat,
            Double lng,
            String firstImageUrl,
            String thumbnailImageUrl,
            String overview,
            String tags,
            String createdTime,
            String modifiedTime,
            String sourceUrl,
            JsonNode raw
    ) {
    }

    private record CompositeX509TrustManager(
            X509TrustManager defaultTrustManager,
            X509TrustManager additionalTrustManager
    ) implements X509TrustManager {

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws java.security.cert.CertificateException {
            defaultTrustManager.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws java.security.cert.CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (java.security.cert.CertificateException exception) {
                additionalTrustManager.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            java.security.cert.X509Certificate[] defaultIssuers = defaultTrustManager.getAcceptedIssuers();
            java.security.cert.X509Certificate[] additionalIssuers = additionalTrustManager.getAcceptedIssuers();
            java.security.cert.X509Certificate[] issuers = Arrays.copyOf(
                    defaultIssuers,
                    defaultIssuers.length + additionalIssuers.length
            );
            System.arraycopy(additionalIssuers, 0, issuers, defaultIssuers.length, additionalIssuers.length);
            return issuers;
        }
    }
}
