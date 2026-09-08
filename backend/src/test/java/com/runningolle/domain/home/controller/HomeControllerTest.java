package com.runningolle.domain.home.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.home.dto.RecommendedCoursesResponse;
import com.runningolle.domain.home.service.CourseRecommendationService;
import com.runningolle.global.security.jwt.JwtAuthenticationFilter;
import com.runningolle.global.security.oauth.CustomOAuth2UserService;
import com.runningolle.global.security.oauth.OAuth2AuthenticationFailureHandler;
import com.runningolle.global.security.oauth.OAuth2AuthenticationSuccessHandler;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseRecommendationService courseRecommendationService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Test
    void returnsRecommendedCoursesForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        given(courseRecommendationService.getRecommendedCourses(userId, 33.45, 126.57))
                .willReturn(new RecommendedCoursesResponse(List.of(
                        new RecommendedCoursesResponse.RecommendedCourseItem(
                                UUID.randomUUID(),
                                "Aewol Coast Run",
                                new BigDecimal("6.40"),
                                Difficulty.LOW,
                                List.of("COAST", "PHOTO"),
                                new BigDecimal("4.70"),
                                3.2,
                                91.5,
                                null,
                                91.5,
                                "Recommended because it matches your preferred distance."
                        )
                )));

        mockMvc.perform(get("/api/home/recommended-courses")
                        .principal(new TestingAuthenticationToken(userId.toString(), null))
                        .param("latitude", "33.45")
                        .param("longitude", "126.57"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].courseName").value("Aewol Coast Run"))
                .andExpect(jsonPath("$.recommendations[0].distanceFromUserKm").value(3.2))
                .andExpect(jsonPath("$.recommendations[0].baseScore").value(91.5))
                .andExpect(jsonPath("$.recommendations[0].finalScore").value(91.5));

        then(courseRecommendationService).should().getRecommendedCourses(userId, 33.45, 126.57);
    }

    @Test
    void passesNullLocationWhenQueryParametersAreOmitted() throws Exception {
        UUID userId = UUID.randomUUID();
        given(courseRecommendationService.getRecommendedCourses(userId, null, null))
                .willReturn(new RecommendedCoursesResponse(List.of()));

        mockMvc.perform(get("/api/home/recommended-courses")
                        .principal(new TestingAuthenticationToken(userId.toString(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray());

        then(courseRecommendationService).should().getRecommendedCourses(userId, null, null);
    }

    @Test
    void returnsBadRequestWhenServiceRejectsHalfProvidedLocation() throws Exception {
        UUID userId = UUID.randomUUID();
        given(courseRecommendationService.getRecommendedCourses(userId, 33.45, null))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude and longitude must be provided together."));

        mockMvc.perform(get("/api/home/recommended-courses")
                        .principal(new TestingAuthenticationToken(userId.toString(), null))
                        .param("latitude", "33.45"))
                .andExpect(status().isBadRequest());

        then(courseRecommendationService).should().getRecommendedCourses(userId, 33.45, null);
        then(courseRecommendationService).should(never()).getRecommendedCourses(userId, null, null);
    }
}
