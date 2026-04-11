package com.godLife.project.service;

import com.godLife.project.dto.request.myPage.GetUserPwRequestDTO;
import com.godLife.project.mapper.UserMapper;
import com.godLife.project.service.impl.UserServiceImpl;
import com.godLife.project.service.interfaces.jwtInterface.RefreshService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - 비밀번호 초기화 보안 테스트")
class UserServiceFindPwTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshService refreshService;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String TEST_EMAIL  = "test@example.com";
    private static final String TEST_USER_ID = "testUser";
    private static final String NEW_PW       = "NewPass1234!";
    private static final String ENCODED_PW   = "$2a$encoded";

    // ──────────────────────────────────────────────────────────────────────
    // 입력 유효성 검증
    // ──────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("입력 유효성 검증")
    class InputValidationTest {

        @Test
        @DisplayName("userPwConfirm 필드 누락 시 400 반환")
        void missingPwConfirm_returns400() {
            GetUserPwRequestDTO req = buildRequest(NEW_PW, null);

            int result = userService.FindUserPw(req, TEST_EMAIL);

            assertThat(result).isEqualTo(400);
            verify(userMapper, never()).findUserPw(any(), any());
        }

        @Test
        @DisplayName("userPwConfirm 공백 문자열 시 400 반환")
        void blankPwConfirm_returns400() {
            GetUserPwRequestDTO req = buildRequest(NEW_PW, "   ");

            int result = userService.FindUserPw(req, TEST_EMAIL);

            assertThat(result).isEqualTo(400);
            verify(userMapper, never()).findUserPw(any(), any());
        }

        @Test
        @DisplayName("비밀번호와 확인 불일치 시 422 반환")
        void pwMismatch_returns422() {
            GetUserPwRequestDTO req = buildRequest(NEW_PW, "DifferentPw!");

            int result = userService.FindUserPw(req, TEST_EMAIL);

            assertThat(result).isEqualTo(422);
            verify(userMapper, never()).findUserPw(any(), any());
        }

        @Test
        @DisplayName("미등록 이메일 시 404 반환")
        void unregisteredEmail_returns404() {
            GetUserPwRequestDTO req = buildRequest(NEW_PW, NEW_PW);
            when(userMapper.checkUserEmailExist(TEST_EMAIL)).thenReturn(false);

            int result = userService.FindUserPw(req, TEST_EMAIL);

            assertThat(result).isEqualTo(404);
            verify(userMapper, never()).findUserPw(any(), any());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 비밀번호 변경 성공 — 세션 무효화
    // ──────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("비밀번호 변경 성공 후 Refresh Token 무효화 (세션 하이재킹 방어)")
    class SessionInvalidationTest {

        @Test
        @DisplayName("변경 성공 시 200 반환 + deleteByUsername 호출로 기존 세션 전체 무효화")
        void success_returns200AndInvalidatesAllSessions() {
            GetUserPwRequestDTO req = buildRequest(NEW_PW, NEW_PW);
            when(userMapper.checkUserEmailExist(TEST_EMAIL)).thenReturn(true);
            when(passwordEncoder.encode(NEW_PW)).thenReturn(ENCODED_PW);
            when(userMapper.findUserPw(ENCODED_PW, TEST_EMAIL)).thenReturn(1);
            when(userMapper.getUserIdByUserEmail(TEST_EMAIL)).thenReturn(TEST_USER_ID);

            int result = userService.FindUserPw(req, TEST_EMAIL);

            assertThat(result).isEqualTo(200);
            verify(refreshService).deleteByUsername(TEST_USER_ID);
        }

        @Test
        @DisplayName("DB 업데이트 결과 0 (대상 없음) 시 404 반환 + Refresh Token 삭제 안 함")
        void dbUpdateReturnsZero_returns404AndNoSessionInvalidation() {
            GetUserPwRequestDTO req = buildRequest(NEW_PW, NEW_PW);
            when(userMapper.checkUserEmailExist(TEST_EMAIL)).thenReturn(true);
            when(passwordEncoder.encode(NEW_PW)).thenReturn(ENCODED_PW);
            when(userMapper.findUserPw(ENCODED_PW, TEST_EMAIL)).thenReturn(0);

            int result = userService.FindUserPw(req, TEST_EMAIL);

            assertThat(result).isEqualTo(404);
            verify(refreshService, never()).deleteByUsername(any());
        }

        @Test
        @DisplayName("getUserIdByUserEmail 이 null 반환해도 deleteByUsername 호출 안 함 — NPE 없이 200")
        void userIdNull_skipsTokenDeletion_returns200() {
            GetUserPwRequestDTO req = buildRequest(NEW_PW, NEW_PW);
            when(userMapper.checkUserEmailExist(TEST_EMAIL)).thenReturn(true);
            when(passwordEncoder.encode(NEW_PW)).thenReturn(ENCODED_PW);
            when(userMapper.findUserPw(ENCODED_PW, TEST_EMAIL)).thenReturn(1);
            when(userMapper.getUserIdByUserEmail(TEST_EMAIL)).thenReturn(null);

            int result = userService.FindUserPw(req, TEST_EMAIL);

            assertThat(result).isEqualTo(200);
            verify(refreshService, never()).deleteByUsername(any());
        }

        @Test
        @DisplayName("예외 발생 시 500 반환 + Refresh Token 삭제 안 함")
        void exceptionDuringUpdate_returns500AndNoSessionInvalidation() {
            GetUserPwRequestDTO req = buildRequest(NEW_PW, NEW_PW);
            when(userMapper.checkUserEmailExist(TEST_EMAIL)).thenReturn(true);
            when(passwordEncoder.encode(NEW_PW)).thenReturn(ENCODED_PW);
            when(userMapper.findUserPw(ENCODED_PW, TEST_EMAIL)).thenThrow(new RuntimeException("DB error"));

            int result = userService.FindUserPw(req, TEST_EMAIL);

            assertThat(result).isEqualTo(500);
            verify(refreshService, never()).deleteByUsername(any());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 아이디 찾기 마스킹
    // ──────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("아이디 찾기 마스킹 처리")
    class FindUserIdMaskingTest {

        @Test
        @DisplayName("isMasked=true: 6자 이하 아이디는 앞 2자 노출")
        void shortId_masked2chars() {
            com.godLife.project.dto.query.user.GetNameNEmail req = new com.godLife.project.dto.query.user.GetNameNEmail();
            req.setUserName("홍길동");
            req.setUserEmail(TEST_EMAIL);
            when(userMapper.getUserId(req)).thenReturn("abc12");

            String result = userService.FindUserIdByNameNEmail(req, true);

            assertThat(result).isEqualTo("ab***");
        }

        @Test
        @DisplayName("isMasked=true: 7~12자 아이디는 앞 3자 노출")
        void mediumId_masked3chars() {
            com.godLife.project.dto.query.user.GetNameNEmail req = new com.godLife.project.dto.query.user.GetNameNEmail();
            req.setUserName("홍길동");
            req.setUserEmail(TEST_EMAIL);
            when(userMapper.getUserId(req)).thenReturn("myUser1");

            String result = userService.FindUserIdByNameNEmail(req, true);

            assertThat(result).isEqualTo("myU****");
        }

        @Test
        @DisplayName("isMasked=true: 13자 이상 아이디는 앞 4자 노출")
        void longId_masked4chars() {
            com.godLife.project.dto.query.user.GetNameNEmail req = new com.godLife.project.dto.query.user.GetNameNEmail();
            req.setUserName("홍길동");
            req.setUserEmail(TEST_EMAIL);
            when(userMapper.getUserId(req)).thenReturn("verylonguserId");

            String result = userService.FindUserIdByNameNEmail(req, true);

            assertThat(result).isEqualTo("very**********");
        }

        @Test
        @DisplayName("isMasked=false: 마스킹 없이 원본 반환")
        void noMasking_returnsOriginal() {
            com.godLife.project.dto.query.user.GetNameNEmail req = new com.godLife.project.dto.query.user.GetNameNEmail();
            req.setUserName("홍길동");
            req.setUserEmail(TEST_EMAIL);
            when(userMapper.getUserId(req)).thenReturn(TEST_USER_ID);

            String result = userService.FindUserIdByNameNEmail(req, false);

            assertThat(result).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("일치하는 유저 없으면 null 반환")
        void noMatch_returnsNull() {
            com.godLife.project.dto.query.user.GetNameNEmail req = new com.godLife.project.dto.query.user.GetNameNEmail();
            req.setUserName("홍길동");
            req.setUserEmail(TEST_EMAIL);
            when(userMapper.getUserId(req)).thenReturn(null);

            String result = userService.FindUserIdByNameNEmail(req, true);

            assertThat(result).isNull();
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────────────────────────────
    private GetUserPwRequestDTO buildRequest(String pw, String confirm) {
        GetUserPwRequestDTO req = new GetUserPwRequestDTO();
        req.setUserEmail(TEST_EMAIL);
        req.setUserPw(pw);
        req.setUserPwConfirm(confirm);
        return req;
    }
}
