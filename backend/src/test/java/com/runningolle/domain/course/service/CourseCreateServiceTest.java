package com.runningolle.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runningolle.domain.course.dto.CourseCreateRequest;
import com.runningolle.domain.course.dto.CourseCreateRequest.WaypointRequest;
import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.entity.CourseTag;
import com.runningolle.domain.course.entity.CourseWaypoint;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.course.repository.CourseRepository;
import com.runningolle.domain.course.repository.CourseTagMapRepository;
import com.runningolle.domain.course.repository.CourseTagRepository;
import com.runningolle.domain.course.repository.CourseThemeRepository;
import com.runningolle.domain.course.repository.CourseWaypointRepository;
import com.runningolle.domain.home.service.CourseRecommendationDocumentService;
import com.runningolle.domain.home.service.CourseRecommendationEmbeddingService;
import com.runningolle.domain.routing.client.OpenRouteServiceClient;
import com.runningolle.domain.routing.client.OpenRouteServiceClient.OrsRouteResult;
import com.runningolle.domain.routing.client.OpenRouteServiceClient.SurfaceBreakdown;
import com.runningolle.domain.routing.client.OpenRouteServiceClient.Waypoint;
import com.runningolle.domain.tourism.client.TourApiClient;
import com.runningolle.domain.tourism.client.TourApiClient.TourDetail;
import com.runningolle.domain.user.entity.Theme;
import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.repository.ThemeRepository;
import com.runningolle.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CourseCreateServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final UUID CREATOR_ID = UUID.randomUUID();

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseWaypointRepository courseWaypointRepository;

    @Mock
    private CourseThemeRepository courseThemeRepository;

    @Mock
    private CourseTagMapRepository courseTagMapRepository;

    @Mock
    private CourseTagRepository courseTagRepository;

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OpenRouteServiceClient openRouteServiceClient;

    @Mock
    private TourApiClient tourApiClient;

    @Mock
    private CourseRecommendationDocumentService courseRecommendationDocumentService;

    @Mock
    private CourseRecommendationEmbeddingService courseRecommendationEmbeddingService;

    private CourseCreateService courseCreateService;

    @BeforeEach
    void setUp() {
        courseCreateService = new CourseCreateService(
                courseRepository,
                courseWaypointRepository,
                courseThemeRepository,
                courseTagMapRepository,
                courseTagRepository,
                themeRepository,
                userRepository,
                openRouteServiceClient,
                tourApiClient,
                new ObjectMapper(),
                courseRecommendationDocumentService,
                courseRecommendationEmbeddingService
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsCourseByRecalculatingRouteAndSavingWaypointsThemesAndTags() {
        UUID courseId = UUID.randomUUID();
        UUID themeId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        User user = User.createKakaoUser("kakao-1");
        Theme theme = org.mockito.Mockito.mock(Theme.class);
        CourseTag courseTag = org.mockito.Mockito.mock(CourseTag.class);

        given(userRepository.findById(CREATOR_ID)).willReturn(Optional.of(user));
        given(themeRepository.findAllById(List.of(themeId))).willReturn(List.of(theme));
        given(courseTagRepository.findAllById(List.of(tagId))).willReturn(List.of(courseTag));
        given(openRouteServiceClient.calculateFootWalkingRoute(anyList()))
                .willReturn(routeResult(4.2, 42, 36, new SurfaceBreakdown(70, 20, 10), List.of(1.4, 2.8)));
        given(tourApiClient.getDetail("tour-1", "12")).willReturn(Optional.of(tourDetail()));
        given(courseRepository.save(any(Course.class))).willAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            ReflectionTestUtils.setField(course, "id", courseId);
            return course;
        });

        CourseCreateRequest request = new CourseCreateRequest(
                " 제주 산책 코스 ",
                "  바다와 오름을 지나는 코스  ",
                CourseType.RUNNING_COURSE,
                List.of(
                        waypoint("kakao-2", "용두암", 33.5161104, 126.5119574, 1, "tour-1", "12"),
                        waypoint("kakao-1", "제주시청", 33.4996213, 126.5311884, 0, null, null),
                        waypoint("kakao-3", "탑동광장", 33.5173, 126.5266, 2, null, null)
                ),
                List.of(themeId),
                List.of(tagId),
                true
        );

        var response = courseCreateService.createCourse(CREATOR_ID, request);

        assertThat(response.courseId()).isEqualTo(courseId);

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(courseCaptor.capture());
        Course savedCourse = courseCaptor.getValue();
        assertThat(savedCourse.getName()).isEqualTo("제주 산책 코스");
        assertThat(savedCourse.getDescription()).isEqualTo("바다와 오름을 지나는 코스");
        assertThat(savedCourse.getDistanceKm()).isEqualByComparingTo(new BigDecimal("4.20"));
        assertThat(savedCourse.getEstimatedDurationMinutes()).isEqualTo(42);
        assertThat(savedCourse.getElevationGainM()).isEqualByComparingTo(new BigDecimal("36.00"));
        assertThat(savedCourse.getDifficulty()).isEqualTo(Difficulty.LOW);
        assertThat(savedCourse.getSurfaceAsphaltPct()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(savedCourse.getSurfaceDirtPct()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(savedCourse.getSurfaceStairsPct()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(savedCourse.getThumbnailImageUrl()).isEqualTo("https://example.com/tour.jpg");
        assertThat(savedCourse.getStartPoint().getY()).isEqualTo(33.4996213);
        assertThat(savedCourse.getStartPoint().getX()).isEqualTo(126.5311884);

        ArgumentCaptor<List<Waypoint>> routeWaypointsCaptor = ArgumentCaptor.forClass(List.class);
        verify(openRouteServiceClient).calculateFootWalkingRoute(routeWaypointsCaptor.capture());
        assertThat(routeWaypointsCaptor.getValue())
                .extracting(Waypoint::name)
                .containsExactly("제주시청", "용두암", "탑동광장");

        ArgumentCaptor<Iterable<CourseWaypoint>> waypointCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(courseWaypointRepository).saveAll(waypointCaptor.capture());
        List<CourseWaypoint> savedWaypoints = StreamSupport.stream(waypointCaptor.getValue().spliterator(), false)
                .toList();
        assertThat(savedWaypoints)
                .hasSize(3)
                .extracting(
                        CourseWaypoint::getName,
                        CourseWaypoint::getOrderIndex,
                        CourseWaypoint::getDistanceFromStartKm
                )
                .containsExactly(
                        tuple("제주시청", 0, new BigDecimal("0.00")),
                        tuple("용두암", 1, new BigDecimal("1.40")),
                        tuple("탑동광장", 2, new BigDecimal("4.20"))
                );
        assertThat(savedWaypoints.get(1).getTourContentId()).isEqualTo("tour-1");
        assertThat(savedWaypoints.get(1).getTourContentTypeId()).isEqualTo("12");
        assertThat(savedWaypoints.get(1).getTourData()).isNotNull();
        assertThat(savedWaypoints.get(1).getTourSyncedAt()).isNotNull();

        verify(courseThemeRepository).saveAll(anyList());
        verify(courseTagMapRepository).saveAll(anyList());
        verify(courseRecommendationDocumentService).syncCourseDescriptionDocument(courseId);
        verify(courseRecommendationEmbeddingService).syncCourseRecommendationEmbeddings(courseId);
    }

    @Test
    void doesNotCallOpenRouteServiceWhenThemeIdIsInvalid() {
        UUID unknownThemeId = UUID.randomUUID();
        given(userRepository.findById(CREATOR_ID)).willReturn(Optional.of(User.createKakaoUser("kakao-1")));
        given(themeRepository.findAllById(List.of(unknownThemeId))).willReturn(List.of());

        CourseCreateRequest request = new CourseCreateRequest(
                "코스",
                null,
                CourseType.RUNNING_COURSE,
                List.of(
                        waypoint("kakao-1", "출발", 33.4996213, 126.5311884, 0, null, null),
                        waypoint("kakao-2", "도착", 33.5161104, 126.5119574, 1, null, null)
                ),
                List.of(unknownThemeId),
                List.of(),
                true
        );

        assertThatThrownBy(() -> courseCreateService.createCourse(CREATOR_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("존재하지 않는 테마");

        verify(openRouteServiceClient, never()).calculateFootWalkingRoute(anyList());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void doesNotFetchTourApiDetailWhenWaypointAlreadyHasTourData() {
        User user = User.createKakaoUser("kakao-1");
        given(userRepository.findById(CREATOR_ID)).willReturn(Optional.of(user));
        given(openRouteServiceClient.calculateFootWalkingRoute(anyList()))
                .willReturn(routeResult(2.0, 20, 5, null, List.of(2.0)));
        given(courseRepository.save(any(Course.class))).willAnswer(invocation -> invocation.getArgument(0));

        WaypointRequest waypointWithTourData = new WaypointRequest(
                "kakao-1",
                "용두암",
                33.5161104,
                126.5119574,
                0,
                null,
                "tour-1",
                "12",
                null,
                null,
                new ObjectMapper().valueToTree(Map.of("firstimage", "https://example.com/raw.jpg"))
        );
        CourseCreateRequest request = new CourseCreateRequest(
                "코스",
                null,
                CourseType.RUNNING_COURSE,
                List.of(
                        waypointWithTourData,
                        waypoint("kakao-2", "도착", 33.5173, 126.5266, 1, null, null)
                ),
                List.of(),
                List.of(),
                false
        );

        courseCreateService.createCourse(CREATOR_ID, request);

        verify(tourApiClient, never()).getDetail(any(), any());
    }

    private static WaypointRequest waypoint(
            String kakaoPlaceId,
            String name,
            double lat,
            double lng,
            int orderIndex,
            String tourContentId,
            String tourContentTypeId
    ) {
        return new WaypointRequest(
                kakaoPlaceId,
                name,
                lat,
                lng,
                orderIndex,
                null,
                tourContentId,
                tourContentTypeId,
                null,
                null,
                null
        );
    }

    private static OrsRouteResult routeResult(
            double distanceKm,
            int estimatedDurationMinutes,
            double elevationGainM,
            SurfaceBreakdown surfaceBreakdown,
            List<Double> segmentDistanceKm
    ) {
        LineString lineString = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(126.5311884, 33.4996213),
                new Coordinate(126.5119574, 33.5161104),
                new Coordinate(126.5266, 33.5173)
        });
        lineString.setSRID(4326);

        return new OrsRouteResult(
                distanceKm,
                estimatedDurationMinutes,
                elevationGainM,
                surfaceBreakdown,
                lineString,
                "LINESTRING (126.5311884 33.4996213, 126.5119574 33.5161104, 126.5266 33.5173)",
                segmentDistanceKm
        );
    }

    private static TourDetail tourDetail() {
        return new TourDetail(
                "tour-1",
                "12",
                "용두암",
                "제주특별자치도 제주시",
                null,
                "39",
                "4",
                "A01",
                "A0101",
                "A01010100",
                33.5161104,
                126.5119574,
                "제주 관광지 설명",
                "https://example.com/tour.jpg",
                "상시 이용",
                Map.of("detailCommon2", Map.of("contentid", "tour-1"))
        );
    }
}
