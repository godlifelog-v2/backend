package com.godLife.project.jwt;

import com.godLife.project.dto.model.user.UserDTO;
import com.godLife.project.service.interfaces.UserService;
import com.godLife.project.service.interfaces.jwtInterface.RefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginFilter - isBanned 차단 로직 테스트")
class LoginFilterBanTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JWTUtil jwtUtil;
    @Mock private RefreshService refreshService;
    @Mock private UserService userService;

    private LoginFilter loginFilter;

    @BeforeEach
    void setUp() {
        loginFilter = new LoginFilter(authenticationManager, jwtUtil, refreshService, userService);
    }

    private Authentication mockAuthentication(String username) {
        return new UsernamePasswordAuthenticationToken(
                username, null,
                List.of(new SimpleGrantedAuthority("1"))
        );
    }

    private UserDTO buildUser(int isBanned, int authorityIdx) {
        UserDTO user = new UserDTO();
        user.setIsBanned(isBanned);
        user.setAuthorityIdx(authorityIdx);
        user.setUserNick("testNick");
        user.setNickTag("#1");
        return user;
    }

    @Nested
    @DisplayName("정지 유저 차단")
    class BannedUserTest {

        @Test
        @DisplayName("isBanned=1 유저 로그인 시 403 응답 + 토큰 미발급")
        void bannedUser_returns403_noTokenIssued() throws IOException {
            when(userService.findByUserId("user1")).thenReturn(buildUser(1, 1));

            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            loginFilter.successfulAuthentication(request, response, new MockFilterChain(), mockAuthentication("user1"));

            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getContentAsString()).contains("정지된 계정입니다.");
            verify(jwtUtil, never()).createJwt(any(), any(), any(), anyLong());
            verify(refreshService, never()).addRefreshToken(any(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("정상 유저 로그인")
    class NormalUserTest {

        @Test
        @DisplayName("isBanned=0 일반 유저 로그인 시 200 + access 토큰 발급")
        void normalUser_returns200_withToken() throws IOException {
            when(userService.findByUserId("user1")).thenReturn(buildUser(0, 1));
            when(jwtUtil.createJwt(eq("access"), any(), any(), anyLong())).thenReturn("access-token");
            when(jwtUtil.createJwt(eq("refresh"), any(), any(), anyLong())).thenReturn("refresh-token");

            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            loginFilter.successfulAuthentication(request, response, new MockFilterChain(), mockAuthentication("user1"));

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("Authorization")).isEqualTo("Bearer access-token");
            verify(refreshService).addRefreshToken(eq("user1"), eq("refresh-token"), anyLong());
        }

        @Test
        @DisplayName("일반 유저(authorityIdx=1) 로그인 시 roleStatus=false")
        void normalUser_roleStatusFalse() throws IOException {
            when(userService.findByUserId("user1")).thenReturn(buildUser(0, 1));
            when(jwtUtil.createJwt(any(), any(), any(), anyLong())).thenReturn("token");

            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            loginFilter.successfulAuthentication(request, response, new MockFilterChain(), mockAuthentication("user1"));

            assertThat(response.getContentAsString()).contains("\"roleStatus\":false");
        }

        @Test
        @DisplayName("관리자 유저(authorityIdx=2) 로그인 시 roleStatus=true")
        void adminUser_roleStatusTrue() throws IOException {
            when(userService.findByUserId("admin1")).thenReturn(buildUser(0, 2));
            when(jwtUtil.createJwt(any(), any(), any(), anyLong())).thenReturn("token");

            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            loginFilter.successfulAuthentication(request, response, new MockFilterChain(), mockAuthentication("admin1"));

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString()).contains("\"roleStatus\":true");
        }
    }
}
