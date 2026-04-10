package com.godLife.project.service;

import com.godLife.project.dto.model.user.UserDTO;
import com.godLife.project.jwt.JWTUtil;
import com.godLife.project.service.impl.jwtImpl.ReissueService;
import com.godLife.project.service.interfaces.UserService;
import com.godLife.project.service.interfaces.jwtInterface.RefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReissueService - 토큰 재발급 로직 테스트")
class ReissueServiceTest {

    @Mock private JWTUtil jwtUtil;
    @Mock private RefreshService refreshService;
    @Mock private UserService userService;

    @InjectMocks
    private ReissueService reissueService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String VALID_REFRESH = "valid.refresh.token";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setCookies(new Cookie("refresh", VALID_REFRESH));
    }

    private void setupValidToken(String username) {
        when(jwtUtil.isExpired(VALID_REFRESH)).thenReturn(false);
        when(jwtUtil.getCategory(VALID_REFRESH)).thenReturn("refresh");
        when(refreshService.existsByRefresh(VALID_REFRESH)).thenReturn(true);
        when(jwtUtil.getUsername(VALID_REFRESH)).thenReturn(username);
        when(jwtUtil.getRole(VALID_REFRESH)).thenReturn("1");
    }

    @Nested
    @DisplayName("정지 유저 재발급 차단")
    class BannedUserReissueTest {

        @Test
        @DisplayName("정지 유저(isBanned=1)의 토큰 재발급 시 403 반환 + refresh 토큰 삭제")
        void bannedUser_reissue_returns403() {
            setupValidToken("bannedUser");

            UserDTO bannedUser = new UserDTO();
            bannedUser.setIsBanned(1);
            when(userService.findByUserId("bannedUser")).thenReturn(bannedUser);

            ResponseEntity<?> result = reissueService.reissueToken(request, response);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(refreshService).deleteByRefresh(VALID_REFRESH);
            verify(jwtUtil, never()).createJwt(any(), any(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("정상 유저 재발급")
    class NormalUserReissueTest {

        @Test
        @DisplayName("정상 유저(isBanned=0) 토큰 재발급 시 200 + 새 토큰 발급")
        void normalUser_reissue_returns200() {
            setupValidToken("normalUser");

            UserDTO normalUser = new UserDTO();
            normalUser.setIsBanned(0);
            when(userService.findByUserId("normalUser")).thenReturn(normalUser);
            when(jwtUtil.createJwt(eq("access"), any(), any(), anyLong())).thenReturn("new-access");
            when(jwtUtil.createJwt(eq("refresh"), any(), any(), anyLong())).thenReturn("new-refresh");

            ResponseEntity<?> result = reissueService.reissueToken(request, response);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeader("Authorization")).isEqualTo("Bearer new-access");
            verify(refreshService).deleteByRefresh(VALID_REFRESH);
            verify(refreshService).addRefreshToken(eq("normalUser"), eq("new-refresh"), anyLong());
        }
    }

    @Nested
    @DisplayName("토큰 유효성 검증")
    class TokenValidationTest {

        @Test
        @DisplayName("refresh 쿠키 없을 시 400 반환")
        void noRefreshCookie_returns400() {
            request = new MockHttpServletRequest(); // 쿠키 없음

            ResponseEntity<?> result = reissueService.reissueToken(request, response);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("DB에 없는 refresh 토큰 재발급 시 401 반환")
        void refreshNotInDB_returns401() {
            when(jwtUtil.isExpired(VALID_REFRESH)).thenReturn(false);
            when(jwtUtil.getCategory(VALID_REFRESH)).thenReturn("refresh");
            when(refreshService.existsByRefresh(VALID_REFRESH)).thenReturn(false);

            ResponseEntity<?> result = reissueService.reissueToken(request, response);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
