package com.runningolle.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.domain.place.client.KakaoPlaceClient;
import com.runningolle.domain.place.client.KakaoPlaceClient.KakaoPlace;
import com.runningolle.domain.tourism.entity.TourismPlace;
import com.runningolle.domain.tourism.entity.TourismPlace.TourismPlaceSnapshot;
import com.runningolle.domain.tourism.repository.TourismPlaceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    private KakaoPlaceClient kakaoPlaceClient;

    @Mock
    private TourismPlaceRepository tourismPlaceRepository;

    private PlaceService placeService;

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(kakaoPlaceClient, tourismPlaceRepository);
    }

    @Test
    void searchesKakaoPlacesAndMarksTourismCandidates() {
        given(kakaoPlaceClient.searchKeyword("제주", 33.4996213, 126.5311884, 5_000))
                .willReturn(List.of(
                        kakaoPlace("kakao-tour", "용두암", "AT4", "관광명소", 33.5161104, 126.5119574),
                        kakaoPlace("kakao-food", "제주식당", "FD6", "음식점", 33.5, 126.53)
        ));
        given(tourismPlaceRepository.searchNearbyOfficialTourismPlaces("제주", 33.4996213, 126.5311884, 5_000, 10))
                .willReturn(List.of());
        given(tourismPlaceRepository.searchOfficialTourismPlacesByKeyword("제주", 10))
                .willReturn(List.of());

        var response = placeService.searchPlaces("제주", 33.4996213, 126.5311884, null);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).isTourismCandidate()).isTrue();
        assertThat(response.get(1).isTourismCandidate()).isFalse();
        assertThat(response.get(0).address()).isEqualTo("제주로 1");
    }

    @Test
    void includesOfficialTourismPlacesFromCacheInSearchResults() {
        TourismPlace tourismPlace = tourismPlace("tour-saryeoni", "사려니숲길", 33.421530, 126.626488);
        given(kakaoPlaceClient.searchKeyword("사려니", 33.421530, 126.626488, 5_000))
                .willReturn(List.of());
        given(kakaoPlaceClient.searchKeywordInJeju("사려니"))
                .willReturn(List.of());
        given(tourismPlaceRepository.searchNearbyOfficialTourismPlaces("사려니", 33.421530, 126.626488, 5_000, 10))
                .willReturn(List.of(tourismPlace));
        given(tourismPlaceRepository.searchOfficialTourismPlacesByKeyword("사려니", 10))
                .willReturn(List.of());

        var response = placeService.searchPlaces("사려니", 33.421530, 126.626488, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).kakaoPlaceId()).isEqualTo("tourapi:tour-saryeoni");
        assertThat(response.get(0).name()).isEqualTo("사려니숲길");
        assertThat(response.get(0).categoryGroupCode()).isEqualTo("AT4");
        assertThat(response.get(0).categoryName()).isEqualTo("관광지");
        assertThat(response.get(0).isTourismCandidate()).isTrue();
    }

    @Test
    void skipsOfficialTourismSearchResultWhenKakaoAlreadyHasSamePlace() {
        KakaoPlace kakaoPlace = kakaoPlace("kakao-halla", "한라산", "AT4", "관광명소", 33.361667, 126.529167);
        TourismPlace tourismPlace = tourismPlace("tour-halla", "한라산국립공원", 33.361667, 126.529167);
        given(kakaoPlaceClient.searchKeyword("한라산", 33.361667, 126.529167, 5_000))
                .willReturn(List.of(kakaoPlace));
        given(tourismPlaceRepository.searchNearbyOfficialTourismPlaces("한라산", 33.361667, 126.529167, 5_000, 10))
                .willReturn(List.of(tourismPlace));
        given(tourismPlaceRepository.searchOfficialTourismPlacesByKeyword("한라산", 10))
                .willReturn(List.of());

        var response = placeService.searchPlaces("한라산", 33.361667, 126.529167, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).kakaoPlaceId()).isEqualTo("kakao-halla");
    }

    @Test
    void includesOfficialTourismKeywordResultWhenPlaceIsOutsideNearbyRadius() {
        KakaoPlace unrelatedRestaurant = kakaoPlace("kakao-lilis", "리리스", "FD6", "음식점", 33.489, 126.488);
        TourismPlace tourismPlace = tourismPlace("tour-seongsan", "성산일출봉", 33.462147, 126.936424);
        given(kakaoPlaceClient.searchKeyword("성산일출봉", 37.497952, 127.027619, 5_000))
                .willReturn(List.of(unrelatedRestaurant));
        given(kakaoPlaceClient.searchKeywordInJeju("성산일출봉"))
                .willReturn(List.of());
        given(tourismPlaceRepository.searchNearbyOfficialTourismPlaces("성산일출봉", 37.497952, 127.027619, 5_000, 10))
                .willReturn(List.of());
        given(tourismPlaceRepository.searchOfficialTourismPlacesByKeyword("성산일출봉", 10))
                .willReturn(List.of(tourismPlace));

        var response = placeService.searchPlaces("성산일출봉", 37.497952, 127.027619, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).kakaoPlaceId()).isEqualTo("tourapi:tour-seongsan");
        assertThat(response.get(0).name()).isEqualTo("성산일출봉");
    }

    @Test
    void ranksExactTourismPlaceBeforeKeywordContainingAmenities() {
        given(kakaoPlaceClient.searchKeyword("성산일출봉", 33.462147, 126.936424, 5_000))
                .willReturn(List.of(
                        kakaoPlace("kakao-parking", "성산일출봉 주차장", "PK6", "주차장", 33.4620, 126.9355),
                        kakaoPlace("kakao-market", "성산일출봉농협 하나로마트", "CS2", "대형마트", 33.4630, 126.9340),
                        kakaoPlace("kakao-tour", "성산일출봉", "AT4", "관광명소", 33.462147, 126.936424)
                ));
        given(tourismPlaceRepository.searchNearbyOfficialTourismPlaces("성산일출봉", 33.462147, 126.936424, 5_000, 10))
                .willReturn(List.of());
        given(tourismPlaceRepository.searchOfficialTourismPlacesByKeyword("성산일출봉", 10))
                .willReturn(List.of());

        var response = placeService.searchPlaces("성산일출봉", 33.462147, 126.936424, null);

        assertThat(response).extracting("name")
                .containsExactly("성산일출봉", "성산일출봉농협 하나로마트", "성산일출봉 주차장");
    }

    @Test
    void infersKakaoTourismCategoryAndDeduplicatesOfficialTourismCluster() {
        KakaoPlace kakaoPlace = kakaoPlace(
                "kakao-seongsan",
                "성산일출봉",
                "",
                "여행 > 관광,명소 > 산봉우리",
                33.462147,
                126.936424
        );
        TourismPlace tourismPlace = tourismPlace(
                "tour-seongsan",
                "성산일출봉 [유네스코 세계자연유산]",
                33.4599,
                126.9406
        );
        given(kakaoPlaceClient.searchKeyword("성산일출봉", 33.462147, 126.936424, 5_000))
                .willReturn(List.of(kakaoPlace));
        given(tourismPlaceRepository.searchNearbyOfficialTourismPlaces("성산일출봉", 33.462147, 126.936424, 5_000, 10))
                .willReturn(List.of(tourismPlace));
        given(tourismPlaceRepository.searchOfficialTourismPlacesByKeyword("성산일출봉", 10))
                .willReturn(List.of(tourismPlace));

        var response = placeService.searchPlaces("성산일출봉", 33.462147, 126.936424, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).kakaoPlaceId()).isEqualTo("kakao-seongsan");
        assertThat(response.get(0).categoryGroupCode()).isEqualTo("AT4");
        assertThat(response.get(0).isTourismCandidate()).isTrue();
    }

    @Test
    void keepsOfficialTourismDestinationWhenPartialKeywordFindsBroadParkName() {
        KakaoPlace broadPark = kakaoPlace(
                "kakao-seongsan-park",
                "성산일출해양도립공원",
                "AT4",
                "관광명소",
                33.4599,
                126.9406
        );
        TourismPlace tourismPlace = tourismPlace(
                "tour-seongsan",
                "성산일출봉 [유네스코 세계자연유산]",
                33.462147,
                126.936424
        );
        given(kakaoPlaceClient.searchKeyword("성산일출", 33.462147, 126.936424, 5_000))
                .willReturn(List.of(broadPark));
        given(tourismPlaceRepository.searchNearbyOfficialTourismPlaces("성산일출", 33.462147, 126.936424, 5_000, 10))
                .willReturn(List.of(tourismPlace));
        given(tourismPlaceRepository.searchOfficialTourismPlacesByKeyword("성산일출", 10))
                .willReturn(List.of(tourismPlace));

        var response = placeService.searchPlaces("성산일출", 33.462147, 126.936424, null);

        assertThat(response).extracting("name")
                .containsExactly("성산일출봉 [유네스코 세계자연유산]", "성산일출해양도립공원");
    }

    @Test
    void nearbyCategorySearchUsesCategoryAroundAnchor() {
        KakaoPlace cafe = kakaoPlace("kakao-cafe", "성산 카페", "CE7", "카페", 33.463, 126.935);
        given(kakaoPlaceClient.searchKeyword("카페", 33.462147, 126.936424, 1_500, "CE7"))
                .willReturn(List.of(cafe));

        var response = placeService.searchNearbyPlaces(33.462147, 126.936424, null, "CE7");

        assertThat(response).hasSize(1);
        assertThat(response.get(0).kakaoPlaceId()).isEqualTo("kakao-cafe");
        assertThat(response.get(0).categoryGroupCode()).isEqualTo("CE7");
    }

    @Test
    void fallsBackToJejuScopedKakaoSearchWhenNearbyKakaoSearchHasNoResult() {
        KakaoPlace kakaoPlace = kakaoPlace("kakao-cafe", "성산 카페", "CE7", "카페", 33.462, 126.936);
        given(kakaoPlaceClient.searchKeyword("성산 카페", 37.497952, 127.027619, 5_000))
                .willReturn(List.of());
        given(kakaoPlaceClient.searchKeywordInJeju("성산 카페"))
                .willReturn(List.of(kakaoPlace));
        given(tourismPlaceRepository.searchNearbyOfficialTourismPlaces("성산 카페", 37.497952, 127.027619, 5_000, 10))
                .willReturn(List.of());
        given(tourismPlaceRepository.searchOfficialTourismPlacesByKeyword("성산 카페", 10))
                .willReturn(List.of());

        var response = placeService.searchPlaces("성산 카페", 37.497952, 127.027619, null);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).kakaoPlaceId()).isEqualTo("kakao-cafe");
        assertThat(response.get(0).categoryGroupCode()).isEqualTo("CE7");
    }

    @Test
    void returnsTourApiDetailDirectlyForOfficialTourismSearchResult() {
        TourismPlace tourismPlace = tourismPlace("tour-halla", "한라산국립공원", 33.361667, 126.529167);
        given(tourismPlaceRepository.findByContentId("tour-halla")).willReturn(Optional.of(tourismPlace));
        given(kakaoPlaceClient.searchKeyword("한라산국립공원", 33.361667, 126.529167, 2_000))
                .willReturn(List.of());

        var response = placeService.getPlaceDetail(
                "tourapi:tour-halla",
                "한라산국립공원",
                33.361667,
                126.529167,
                "AT4"
        );

        assertThat(response.kakaoPlaceId()).isEqualTo("tourapi:tour-halla");
        assertThat(response.tourApiMatched()).isTrue();
        assertThat(response.tourContentId()).isEqualTo("tour-halla");
        assertThat(response.name()).isEqualTo("한라산국립공원");
        assertThat(response.categoryName()).isEqualTo("관광지");
    }

    @Test
    void enrichesOfficialTourismDetailWithMatchingKakaoPlaceForRoutingFriendlyCoordinate() {
        TourismPlace tourismPlace = tourismPlace(
                "tour-seongsan",
                "성산일출봉 [유네스코 세계자연유산]",
                33.4599,
                126.9406
        );
        KakaoPlace kakaoPlace = kakaoPlace(
                "kakao-seongsan",
                "성산일출봉",
                "",
                "여행 > 관광,명소 > 산봉우리",
                33.462147,
                126.936424
        );
        given(tourismPlaceRepository.findByContentId("tour-seongsan")).willReturn(Optional.of(tourismPlace));
        given(kakaoPlaceClient.searchKeyword("성산일출봉", 33.4599, 126.9406, 2_000))
                .willReturn(List.of(kakaoPlace));

        var response = placeService.getPlaceDetail(
                "tourapi:tour-seongsan",
                "성산일출봉 [유네스코 세계자연유산]",
                33.4599,
                126.9406,
                "AT4"
        );

        assertThat(response.kakaoPlaceId()).isEqualTo("kakao-seongsan");
        assertThat(response.name()).isEqualTo("성산일출봉");
        assertThat(response.lat()).isEqualTo(33.462147);
        assertThat(response.lng()).isEqualTo(126.936424);
        assertThat(response.tourApiMatched()).isTrue();
        assertThat(response.tourContentId()).isEqualTo("tour-seongsan");
    }

    @Test
    void returnsTourApiDetailWhenTourismCacheMatchesKakaoPlace() {
        KakaoPlace kakaoPlace = kakaoPlace("kakao-halla", "한라산", "AT4", "관광명소", 33.361667, 126.529167);
        TourismPlace tourismPlace = tourismPlace("tour-halla", "한라산국립공원", 33.361667, 126.529167);
        given(kakaoPlaceClient.searchKeyword("한라산", 33.361667, 126.529167, 1_000, "AT4"))
                .willReturn(List.of(kakaoPlace));
        given(tourismPlaceRepository.findNearbyOfficialTourismPlaces(33.361667, 126.529167, 2_000.0, 20))
                .willReturn(List.of(tourismPlace));

        var response = placeService.getPlaceDetail("kakao-halla", "한라산", 33.361667, 126.529167, "AT4");

        assertThat(response.tourApiMatched()).isTrue();
        assertThat(response.tourContentId()).isEqualTo("tour-halla");
        assertThat(response.tourContentTypeId()).isEqualTo("12");
        assertThat(response.overview()).isEqualTo("TourAPI overview");
        assertThat(response.firstImageUrl()).isEqualTo("https://example.com/tour.jpg");
        assertThat(response.useTime()).isEqualTo("09:00 - 18:00");
        assertThat(response.tourDataRaw()).isNotNull();
    }

    @Test
    void returnsKakaoOnlyDetailWhenTourismCacheDoesNotMatch() {
        KakaoPlace kakaoPlace = kakaoPlace("kakao-tour", "용두암", "AT4", "관광명소", 33.5161104, 126.5119574);
        given(kakaoPlaceClient.searchKeyword("용두암", 33.5161104, 126.5119574, 1_000, "AT4"))
                .willReturn(List.of(kakaoPlace));
        given(tourismPlaceRepository.findNearbyOfficialTourismPlaces(33.5161104, 126.5119574, 2_000.0, 20))
                .willReturn(List.of());

        var response = placeService.getPlaceDetail("kakao-tour", "용두암", 33.5161104, 126.5119574, "AT4");

        assertThat(response.tourApiMatched()).isFalse();
        assertThat(response.tourContentId()).isNull();
        assertThat(response.name()).isEqualTo("용두암");
        assertThat(response.phone()).isEqualTo("064-000-0000");
    }

    @Test
    void doesNotTryTourApiCacheForNonTourismKakaoCategory() {
        KakaoPlace kakaoPlace = kakaoPlace("kakao-food", "제주식당", "FD6", "음식점", 33.5, 126.53);
        given(kakaoPlaceClient.searchKeyword("제주식당", 33.5, 126.53, 1_000, "FD6"))
                .willReturn(List.of(kakaoPlace));

        var response = placeService.getPlaceDetail("kakao-food", "제주식당", 33.5, 126.53, "FD6");

        assertThat(response.tourApiMatched()).isFalse();
        assertThat(response.categoryName()).isEqualTo("음식점");
        verify(tourismPlaceRepository, never()).findNearbyOfficialTourismPlaces(
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyInt()
        );
    }

    @Test
    void retriesKakaoDetailLookupWithoutCategoryFilterWhenExactPlaceIsNotFound() {
        KakaoPlace kakaoPlace = kakaoPlace("kakao-cafe", "제주카페", "CE7", "카페", 33.5, 126.53);
        given(kakaoPlaceClient.searchKeyword("제주카페", 33.5, 126.53, 1_000, "AT4"))
                .willReturn(List.of());
        given(kakaoPlaceClient.searchKeyword("제주카페", 33.5, 126.53, 1_000))
                .willReturn(List.of(kakaoPlace));

        var response = placeService.getPlaceDetail("kakao-cafe", "제주카페", 33.5, 126.53, "AT4");

        assertThat(response.kakaoPlaceId()).isEqualTo("kakao-cafe");
        assertThat(response.categoryName()).isEqualTo("카페");
        assertThat(response.tourApiMatched()).isFalse();
    }

    private static KakaoPlace kakaoPlace(
            String kakaoPlaceId,
            String name,
            String categoryGroupCode,
            String categoryName,
            double lat,
            double lng
    ) {
        return new KakaoPlace(
                kakaoPlaceId,
                name,
                categoryGroupCode,
                categoryName,
                categoryName,
                "제주특별자치도 제주시",
                "제주로 1",
                "064-000-0000",
                lat,
                lng,
                "https://place.map.kakao.com/" + kakaoPlaceId,
                120
        );
    }

    private static TourismPlace tourismPlace(String contentId, String title, double lat, double lng) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lng, lat));
        point.setSRID(4326);
        return TourismPlace.create(new TourismPlaceSnapshot(
                contentId,
                "12",
                title,
                "제주특별자치도 제주시",
                null,
                null,
                "A01",
                "A0101",
                "A01010100",
                "39",
                "4",
                point,
                "https://example.com/tour.jpg",
                "https://example.com/thumb.jpg",
                "TourAPI overview",
                "09:00 - 18:00",
                "20240101000000",
                "20240102000000",
                new ObjectMapper().valueToTree(Map.of("provider", "TourAPI")),
                LocalDateTime.now()
        ));
    }
}
