package com.godLife.project.controller.v1;

import com.godLife.project.dto.model.user.UserDTO;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.service.interfaces.PlanService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlanControllerTest {

    @Mock
    private PlanService planService;

    @InjectMocks
    private PlanController planController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(planController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ────────────────────────────────────────────────
    // Bug 1: checkLike - authHeader null 시 NPE 수정
    // ────────────────────────────────────────────────
    @Nested
    @DisplayName("Bug 1: checkLike NPE 수정")
    class CheckLikeTest {

        @Test
        @DisplayName("Authorization 헤더가 없을 때 NPE 없이 200 응답, userIdx=0으로 조회")
        void noAuthHeader_returns200WithDefaultUserIdx() throws Exception {
            when(planService.checkLike(1, 0)).thenReturn(false);

            mockMvc.perform(get("/api/v1/plan/checkLike/1"))
                .andExpect(status().isOk());

            verify(planService).checkLike(1, 0);
        }

        @Test
        @DisplayName("비인증 상태(만료된 토큰 등)면 userIdx=0으로 조회")
        void unauthenticated_usesDefaultUserIdx() throws Exception {
            when(planService.checkLike(1, 0)).thenReturn(false);

            mockMvc.perform(get("/api/v1/plan/checkLike/1")
                    .header("Authorization", "Bearer expired.token.value"))
                .andExpect(status().isOk());

            verify(planService).checkLike(1, 0);
        }

        @Test
        @DisplayName("인증된 유저이면 실제 userIdx로 조회")
        void authenticatedUser_usesRealUserIdx() throws Exception {
            int expectedUserIdx = 42;
            UserDTO userDTO = new UserDTO();
            userDTO.setUserIdx(expectedUserIdx);
            CustomUserDetails principal = new CustomUserDetails(userDTO, expectedUserIdx);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList())
            );

            when(planService.checkLike(1, expectedUserIdx)).thenReturn(true);

            mockMvc.perform(get("/api/v1/plan/checkLike/1"))
                .andExpect(status().isOk());

            verify(planService).checkLike(1, expectedUserIdx);
        }
    }

    // ────────────────────────────────────────────────
    // Bug 2: detail - 쿠키 파싱 AIOOBE / NFE 수정
    // ────────────────────────────────────────────────
    @Nested
    @DisplayName("Bug 2: 쿠키 파싱 예외 수정")
    class DetailCookieParsingTest {

        @BeforeEach
        void commonSetup() {
            // planDTO=null → 404 분기로 이동
            when(planService.detailRoutine(anyInt(), anyInt(), anyInt())).thenReturn(null);
        }

        @Test
        @DisplayName("정상 쿠키(trailing underscore 포함)에서 AIOOBE 없이 처리")
        void validCookieWithTrailingUnderscore_noException() throws Exception {
            // 형식: planId_timestamp_ (trailing _)
            String cookie = "1_" + (System.currentTimeMillis() - 7_200_000L) + "_";

            mockMvc.perform(get("/api/v1/plan/detail/1")
                    .cookie(new Cookie("viewed_plans", cookie)))
                .andExpect(status().isNotFound()); // 예외 없이 404 반환
        }

        @Test
        @DisplayName("숫자가 아닌 값이 포함된 쿠키에서 NumberFormatException 없이 처리")
        void malformedCookieWithNonNumericValues_noException() throws Exception {
            String malformedCookie = "abc_def_xyz_123_";

            mockMvc.perform(get("/api/v1/plan/detail/1")
                    .cookie(new Cookie("viewed_plans", malformedCookie)))
                .andExpect(status().isNotFound()); // 예외 없이 404 반환
        }

        @Test
        @DisplayName("홀수 길이 배열(trailing _ 없음)에서 AIOOBE 없이 처리")
        void oddLengthCookieArray_noArrayIndexOutOfBoundsException() throws Exception {
            // "1_12345_2" → split("_") → ["1", "12345", "2"] 홀수 길이
            String oddCookie = "1_12345_2";

            mockMvc.perform(get("/api/v1/plan/detail/1")
                    .cookie(new Cookie("viewed_plans", oddCookie)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("빈 쿠키 값에서 예외 없이 처리")
        void emptyCookie_noException() throws Exception {
            mockMvc.perform(get("/api/v1/plan/detail/1")
                    .cookie(new Cookie("viewed_plans", "")))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("쿠키 없을 때도 정상 처리")
        void noCookie_noException() throws Exception {
            mockMvc.perform(get("/api/v1/plan/detail/1"))
                .andExpect(status().isNotFound());
        }
    }
}
