package com.runningolle.domain.home.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.runningolle.domain.home.config.HomeRecommendationProperties;
import com.runningolle.domain.home.dto.CourseRecommendationSyncResponse;
import com.runningolle.domain.home.service.CourseRecommendationSyncService;
import com.runningolle.global.config.SecurityConfig;
import com.runningolle.global.security.jwt.JwtTokenProvider;
import com.runningolle.global.security.oauth.CustomOAuth2UserService;
import com.runningolle.global.security.oauth.OAuth2AuthenticationFailureHandler;
import com.runningolle.global.security.oauth.OAuth2AuthenticationSuccessHandler;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourseRecommendationSyncController.class)
@Import(SecurityConfig.class)
class CourseRecommendationSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseRecommendationSyncService courseRecommendationSyncService;

    @MockBean
    private HomeRecommendationProperties homeRecommendationProperties;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockBean
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncsCourseRecommendations() throws Exception {
        given(homeRecommendationProperties.isManualSyncEnabled()).willReturn(true);
        given(courseRecommendationSyncService.syncPublicCourseRecommendations())
                .willReturn(new CourseRecommendationSyncResponse(
                        3,
                        1,
                        1,
                        0,
                        1,
                        2,
                        0,
                        1,
                        0,
                        LocalDateTime.of(2026, 8, 31, 15, 30)
                ));

        mockMvc.perform(post("/api/admin/home/recommendations/sync/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCourseCount").value(3))
                .andExpect(jsonPath("$.documentCreatedCount").value(1))
                .andExpect(jsonPath("$.embeddingSyncedCount").value(2));

        then(courseRecommendationSyncService).should().syncPublicCourseRecommendations();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsSyncWhenManualSyncIsDisabled() throws Exception {
        given(homeRecommendationProperties.isManualSyncEnabled()).willReturn(false);

        mockMvc.perform(post("/api/admin/home/recommendations/sync/courses"))
                .andExpect(status().isForbidden());

        then(courseRecommendationSyncService).should(never()).syncPublicCourseRecommendations();
    }

    @Test
    void rejectsSyncWhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/admin/home/recommendations/sync/courses"))
                .andExpect(status().isUnauthorized());

        then(courseRecommendationSyncService).should(never()).syncPublicCourseRecommendations();
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejectsSyncWhenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/home/recommendations/sync/courses"))
                .andExpect(status().isForbidden());

        then(courseRecommendationSyncService).should(never()).syncPublicCourseRecommendations();
    }
}
