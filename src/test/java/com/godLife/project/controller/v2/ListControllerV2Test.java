package com.godLife.project.controller.v2;

import com.godLife.project.dto.model.user.UserDTO;
import com.godLife.project.dto.response.plan.v2.MyPlanV2DTO;
import com.godLife.project.dto.response.plan.v2.TodayStatsDTO;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.service.interfaces.v2.ListServiceV2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListControllerV2Test {

    @Mock
    private ListServiceV2 listServiceV2;

    @InjectMocks
    private ListControllerV2 listControllerV2;

    @InjectMocks
    private AnalysisControllerV2 analysisControllerV2;

    private MockMvc mockMvc;
    private MockMvc analysisMvc;
    private final int userIdx = 1;

    @BeforeEach
    void setUp() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserIdx(userIdx);
        CustomUserDetails principal = new CustomUserDetails(userDTO, userIdx);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList())
        );

        mockMvc = MockMvcBuilders.standaloneSetup(listControllerV2)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        analysisMvc = MockMvcBuilders.standaloneSetup(analysisControllerV2)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v2/list/auth/myPlans")
    class MyPlansTest {

        @Test
        @DisplayName("정상 조회 → 200")
        void myPlans_success_returns200() throws Exception {
            MyPlanV2DTO dto = new MyPlanV2DTO();
            when(listServiceV2.getMyPlansList(userIdx)).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v2/list/auth/myPlans"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("루틴 없음 → 204")
        void myPlans_empty_returns204() throws Exception {
            when(listServiceV2.getMyPlansList(userIdx)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v2/list/auth/myPlans"))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("서비스 null 반환 → 500")
        void myPlans_serviceNull_returns500() throws Exception {
            when(listServiceV2.getMyPlansList(userIdx)).thenReturn(null);

            mockMvc.perform(get("/api/v2/list/auth/myPlans"))
                .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v2/list/auth/todayPlans")
    class TodayPlansTest {

        @Test
        @DisplayName("정상 조회 → 200")
        void todayPlans_success_returns200() throws Exception {
            MyPlanV2DTO dto = new MyPlanV2DTO();
            when(listServiceV2.getTodayPlansList(userIdx)).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v2/list/auth/todayPlans"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("오늘 루틴 없음 → 204")
        void todayPlans_empty_returns204() throws Exception {
            when(listServiceV2.getTodayPlansList(userIdx)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v2/list/auth/todayPlans"))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("서비스 null 반환 → 500")
        void todayPlans_serviceNull_returns500() throws Exception {
            when(listServiceV2.getTodayPlansList(userIdx)).thenReturn(null);

            mockMvc.perform(get("/api/v2/list/auth/todayPlans"))
                .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v2/list/auth/todayStats")
    class TodayStatsTest {

        @Test
        @DisplayName("통계 정상 조회 → 200")
        void todayStats_success_returns200() throws Exception {
            TodayStatsDTO stats = new TodayStatsDTO(5, 3, 7);
            when(listServiceV2.getTodayStats(userIdx)).thenReturn(stats);

            analysisMvc.perform(get("/api/v2/analysis/auth/todayStats"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("서비스 예외 → 500")
        void todayStats_exception_returns500() throws Exception {
            when(listServiceV2.getTodayStats(userIdx)).thenThrow(new RuntimeException("DB 오류"));

            analysisMvc.perform(get("/api/v2/analysis/auth/todayStats"))
                .andExpect(status().isInternalServerError());
        }
    }
}
