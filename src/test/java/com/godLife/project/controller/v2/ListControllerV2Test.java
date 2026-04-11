package com.godLife.project.controller.v2;

import com.godLife.project.dto.response.plan.v2.MyPlanV2DTO;
import com.godLife.project.dto.response.plan.v2.TodayStatsDTO;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.service.interfaces.v2.ListServiceV2;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListControllerV2Test {

    @Mock
    private GlobalExceptionHandler handler;

    @Mock
    private ListServiceV2 listServiceV2;

    @InjectMocks
    private ListControllerV2 listControllerV2;

    private MockMvc mockMvc;
    private final String authHeader = "Bearer test-token";
    private final int userIdx = 1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(listControllerV2).build();
        when(handler.getUserIdxFromToken(authHeader)).thenReturn(userIdx);
        when(handler.getHttpStatus(200)).thenReturn(HttpStatus.OK);
        when(handler.getHttpStatus(204)).thenReturn(HttpStatus.NO_CONTENT);
        when(handler.getHttpStatus(500)).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<String, Object> okResponse = new HashMap<>();
        okResponse.put("status", 200);
        when(handler.createResponse(eq(200), any())).thenReturn(okResponse);
        Map<String, Object> errResponse = new HashMap<>();
        errResponse.put("status", 500);
        when(handler.createResponse(eq(500), any())).thenReturn(errResponse);
    }

    @Nested
    @DisplayName("GET /api/v2/list/auth/myPlans")
    class MyPlansTest {

        @Test
        @DisplayName("정상 조회 → 200")
        void myPlans_success_returns200() throws Exception {
            MyPlanV2DTO dto = new MyPlanV2DTO();
            when(listServiceV2.getMyPlansList(userIdx)).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v2/list/auth/myPlans")
                    .header("Authorization", authHeader))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("루틴 없음 → 204")
        void myPlans_empty_returns204() throws Exception {
            when(listServiceV2.getMyPlansList(userIdx)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v2/list/auth/myPlans")
                    .header("Authorization", authHeader))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("서비스 null 반환 → 500")
        void myPlans_serviceNull_returns500() throws Exception {
            when(listServiceV2.getMyPlansList(userIdx)).thenReturn(null);

            mockMvc.perform(get("/api/v2/list/auth/myPlans")
                    .header("Authorization", authHeader))
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

            mockMvc.perform(get("/api/v2/list/auth/todayPlans")
                    .header("Authorization", authHeader))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("오늘 루틴 없음 → 204")
        void todayPlans_empty_returns204() throws Exception {
            when(listServiceV2.getTodayPlansList(userIdx)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v2/list/auth/todayPlans")
                    .header("Authorization", authHeader))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("서비스 null 반환 → 500")
        void todayPlans_serviceNull_returns500() throws Exception {
            when(listServiceV2.getTodayPlansList(userIdx)).thenReturn(null);

            mockMvc.perform(get("/api/v2/list/auth/todayPlans")
                    .header("Authorization", authHeader))
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

            mockMvc.perform(get("/api/v2/list/auth/todayStats")
                    .header("Authorization", authHeader))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("서비스 예외 → 500")
        void todayStats_exception_returns500() throws Exception {
            when(listServiceV2.getTodayStats(userIdx)).thenThrow(new RuntimeException("DB 오류"));

            mockMvc.perform(get("/api/v2/list/auth/todayStats")
                    .header("Authorization", authHeader))
                .andExpect(status().isInternalServerError());
        }
    }
}
