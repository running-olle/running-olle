package com.runningolle.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.assertj.core.groups.Tuple.tuple;

import com.runningolle.domain.course.dto.CourseDraftRouteRequest;
import com.runningolle.domain.course.dto.CourseDraftRouteRequest.WaypointRequest;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.routing.client.OpenRouteServiceClient;
import com.runningolle.domain.routing.client.OpenRouteServiceClient.OrsRouteResult;
import com.runningolle.domain.routing.client.OpenRouteServiceClient.SurfaceBreakdown;
import com.runningolle.domain.routing.client.OpenRouteServiceClient.Waypoint;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CourseDraftRouteServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    private OpenRouteServiceClient openRouteServiceClient;

    private CourseDraftRouteService courseDraftRouteService;

    @BeforeEach
    void setUp() {
        courseDraftRouteService = new CourseDraftRouteService(openRouteServiceClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void calculatesDraftRouteWithSortedWaypointsAndSuggestedDifficulty() {
        given(openRouteServiceClient.calculateFootWalkingRoute(anyList()))
                .willReturn(routeResult(4.2, 42, 36, new SurfaceBreakdown(70, 30, 0)));

        CourseDraftRouteRequest request = new CourseDraftRouteRequest(List.of(
                new WaypointRequest("kakao-2", "성산일출봉", 33.462147, 126.936424, 1),
                new WaypointRequest("kakao-1", "한라산", 33.361667, 126.529167, 0)
        ));

        var response = courseDraftRouteService.calculateDraftRoute(request);

        assertThat(response.distanceKm()).isEqualTo(4.2);
        assertThat(response.estimatedDurationMinutes()).isEqualTo(42);
        assertThat(response.elevationGainM()).isEqualTo(36);
        assertThat(response.surface()).isNotNull();
        assertThat(response.surface().asphaltPct()).isEqualTo(70);
        assertThat(response.surface().dirtPct()).isEqualTo(30);
        assertThat(response.surface().stairsPct()).isZero();
        assertThat(response.routeCoordinates())
                .hasSize(2)
                .extracting(
                        coordinate -> coordinate.lat(),
                        coordinate -> coordinate.lng()
                )
                .containsExactly(
                        tuple(33.4996213, 126.5311884),
                        tuple(33.5161104, 126.5119574)
                );
        assertThat(response.routeLineStringWkt()).startsWith("LINESTRING");
        assertThat(response.suggestedDifficulty()).isEqualTo(Difficulty.LOW);

        ArgumentCaptor<List<Waypoint>> waypointCaptor = ArgumentCaptor.forClass(List.class);
        verify(openRouteServiceClient).calculateFootWalkingRoute(waypointCaptor.capture());
        assertThat(waypointCaptor.getValue())
                .extracting(Waypoint::name)
                .containsExactly("한라산", "성산일출봉");
    }

    @Test
    void keepsSurfaceNullWhenOpenRouteServiceReturnsNoKnownSurface() {
        given(openRouteServiceClient.calculateFootWalkingRoute(anyList()))
                .willReturn(routeResult(5.0, 55, 75, null));

        CourseDraftRouteRequest request = new CourseDraftRouteRequest(List.of(
                new WaypointRequest(null, "현재 위치", 33.4996213, 126.5311884, 0),
                new WaypointRequest("kakao-1", "용두암", 33.5161104, 126.5119574, 1)
        ));

        var response = courseDraftRouteService.calculateDraftRoute(request);

        assertThat(response.surface()).isNull();
        assertThat(response.suggestedDifficulty()).isEqualTo(Difficulty.MID);
    }

    @Test
    void suggestsHighDifficultyByElevationGainPerDistanceOnly() {
        given(openRouteServiceClient.calculateFootWalkingRoute(anyList()))
                .willReturn(routeResult(2.0, 30, 75, new SurfaceBreakdown(100, 0, 0)));

        CourseDraftRouteRequest request = new CourseDraftRouteRequest(List.of(
                new WaypointRequest("kakao-1", "출발지", 33.4996213, 126.5311884, 0),
                new WaypointRequest("kakao-2", "언덕", 33.51, 126.54, 1)
        ));

        var response = courseDraftRouteService.calculateDraftRoute(request);

        assertThat(response.suggestedDifficulty()).isEqualTo(Difficulty.HIGH);
    }

    @Test
    void rejectsRoutesWithLessThanTwoWaypoints() {
        CourseDraftRouteRequest request = new CourseDraftRouteRequest(List.of(
                new WaypointRequest("kakao-1", "출발지", 33.4996213, 126.5311884, 0)
        ));

        assertThatThrownBy(() -> courseDraftRouteService.calculateDraftRoute(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("최소 2개");
    }

    @Test
    void rejectsOnlyConsecutiveDuplicateWaypoints() {
        CourseDraftRouteRequest request = new CourseDraftRouteRequest(List.of(
                new WaypointRequest("kakao-1", "성산일출봉", 33.462147, 126.936424, 0),
                new WaypointRequest("kakao-1", "성산일출봉", 33.462147, 126.936424, 1)
        ));

        assertThatThrownBy(() -> courseDraftRouteService.calculateDraftRoute(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("연속");
    }

    @Test
    void allowsReturningToAnEarlierWaypointAfterVisitingAnotherPlace() {
        given(openRouteServiceClient.calculateFootWalkingRoute(anyList()))
                .willReturn(routeResult(8.4, 84, 72, new SurfaceBreakdown(70, 30, 0)));
        CourseDraftRouteRequest request = new CourseDraftRouteRequest(List.of(
                new WaypointRequest("kakao-1", "성산일출봉", 33.462147, 126.936424, 0),
                new WaypointRequest("kakao-2", "광치기해변", 33.452330, 126.924430, 1),
                new WaypointRequest("kakao-1", "성산일출봉", 33.462147, 126.936424, 2)
        ));

        var response = courseDraftRouteService.calculateDraftRoute(request);

        assertThat(response.distanceKm()).isEqualTo(8.4);
    }

    private static OrsRouteResult routeResult(
            double distanceKm,
            int estimatedDurationMinutes,
            double elevationGainM,
            SurfaceBreakdown surfaceBreakdown
    ) {
        LineString lineString = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(126.5311884, 33.4996213),
                new Coordinate(126.5119574, 33.5161104)
        });
        lineString.setSRID(4326);

        return new OrsRouteResult(
                distanceKm,
                estimatedDurationMinutes,
                elevationGainM,
                surfaceBreakdown,
                lineString,
                "LINESTRING (126.5311884 33.4996213, 126.5119574 33.5161104)",
                List.of(distanceKm)
        );
    }
}
