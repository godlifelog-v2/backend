package com.godLife.project.service;

import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.mapper.UserMapper;
import com.godLife.project.mapper.VerifyMapper;
import com.godLife.project.service.impl.VerifyServiceImpl;
import com.godLife.project.service.impl.redis.RedisService;
import com.godLife.project.service.interfaces.emailInterface.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyServiceImpl - 이메일 인증 로직 보안 테스트")
class VerifyServiceImplTest {

    @Mock private VerifyMapper verifyMapper;
    @Mock private PlanMapper planMapper;
    @Mock private UserMapper userMapper;
    @Mock private EmailService emailService;
    @Mock private RedisService redisService;

    @InjectMocks
    private VerifyServiceImpl verifyService;

    private static final String TEST_EMAIL = "test@example.com";

    // ──────────────────────────────────────────────────────────────────────
    // sendCodeToEmail — 가입/수정용 발송
    // ──────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sendCodeToEmail — 가입/수정용 발송")
    class SendCodeToEmailTest {

        @Test
        @DisplayName("쿨다운 키가 존재하면 IllegalStateException 발생 (429)")
        void cooldownActive_throwsIllegalStateException() {
            when(redisService.checkExistsValue("AuthCode:cooldown:" + TEST_EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> verifyService.sendCodeToEmail(TEST_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잠시 후 다시 시도해주세요.");

            verify(emailService, never()).sendEmail(any(), any(), any());
        }

        @Test
        @DisplayName("쿨다운 없으면 이메일 발송 후 인증코드·쿨다운 키 Redis에 저장")
        void noCooldown_sendsEmailAndSavesRedisKeys() {
            when(redisService.checkExistsValue("AuthCode:cooldown:" + TEST_EMAIL)).thenReturn(false);

            verifyService.sendCodeToEmail(TEST_EMAIL);

            verify(emailService).sendEmail(eq(TEST_EMAIL), any(), any());
            // 인증코드 저장 (5분)
            verify(redisService).saveStringData(startsWith("AuthCode "), any(), eq('m'), eq(5L));
            // 쿨다운 키 저장 (1분)
            verify(redisService).saveStringData(eq("AuthCode:cooldown:" + TEST_EMAIL), eq("1"), eq('m'), eq(1L));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // sendCodeToEmailForFindAccount — 아이디 찾기 / 비밀번호 초기화용 발송
    // ──────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("sendCodeToEmailForFindAccount — 이메일 열거 공격 방어")
    class SendCodeToEmailForFindAccountTest {

        @Test
        @DisplayName("쿨다운 키가 존재하면 IllegalStateException 발생 (429)")
        void cooldownActive_throwsIllegalStateException() {
            when(redisService.checkExistsValue("AuthCode:cooldown:" + TEST_EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> verifyService.sendCodeToEmailForFindAccount(TEST_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잠시 후 다시 시도해주세요.");

            verify(emailService, never()).sendEmail(any(), any(), any());
            // 쿨다운 중이면 쿨다운 키도 재저장 안 함
            verify(redisService, never()).saveStringData(any(), any(), anyChar(), anyLong());
        }

        @Test
        @DisplayName("미등록 이메일이어도 항상 200 — 이메일 발송 안 하고 쿨다운만 적용")
        void unregisteredEmail_noEmailSentButCooldownSet() {
            when(redisService.checkExistsValue("AuthCode:cooldown:" + TEST_EMAIL)).thenReturn(false);
            when(userMapper.checkUserEmailExist(TEST_EMAIL)).thenReturn(false);

            verifyService.sendCodeToEmailForFindAccount(TEST_EMAIL);

            // 이메일 발송 안 함
            verify(emailService, never()).sendEmail(any(), any(), any());
            // 인증코드 저장 안 함
            verify(redisService, never()).saveStringData(startsWith("AuthCode "), any(), anyChar(), anyLong());
            // 쿨다운 키는 저장함 (타이밍 공격 방지)
            verify(redisService).saveStringData(eq("AuthCode:cooldown:" + TEST_EMAIL), eq("1"), eq('m'), eq(1L));
        }

        @Test
        @DisplayName("등록된 이메일이면 이메일 발송 + 인증코드 저장 + 쿨다운 적용")
        void registeredEmail_sendsEmailAndSetsAllKeys() {
            when(redisService.checkExistsValue("AuthCode:cooldown:" + TEST_EMAIL)).thenReturn(false);
            when(userMapper.checkUserEmailExist(TEST_EMAIL)).thenReturn(true);

            verifyService.sendCodeToEmailForFindAccount(TEST_EMAIL);

            verify(emailService).sendEmail(eq(TEST_EMAIL), any(), any());
            verify(redisService).saveStringData(startsWith("AuthCode "), any(), eq('m'), eq(5L));
            verify(redisService).saveStringData(eq("AuthCode:cooldown:" + TEST_EMAIL), eq("1"), eq('m'), eq(1L));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // verifiedAuthCode — Brute Force 방어
    // ──────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("verifiedAuthCode — Brute Force 방어")
    class VerifiedAuthCodeTest {

        private final String CORRECT_CODE = "123456";
        private final String WRONG_CODE   = "000000";
        private final String AUTH_KEY     = "AuthCode " + TEST_EMAIL;
        private final String FAIL_KEY     = "AuthCode:fail:" + TEST_EMAIL;
        private final String VERIFIED_KEY = "EMAIL_VERIFIED: " + TEST_EMAIL;

        @Test
        @DisplayName("올바른 코드 입력 시 true 반환 + 인증코드/실패키 삭제 + 인증 플래그 저장")
        void correctCode_returnsTrueAndCleansUp() {
            when(redisService.getStringData(FAIL_KEY)).thenReturn(null);
            when(redisService.getStringData(AUTH_KEY)).thenReturn(CORRECT_CODE);

            boolean result = verifyService.verifiedAuthCode(TEST_EMAIL, CORRECT_CODE);

            assertThat(result).isTrue();
            verify(redisService).deleteData(AUTH_KEY);
            verify(redisService).deleteData(FAIL_KEY);
            verify(redisService).saveStringData(eq(VERIFIED_KEY), eq("true"), eq('m'), eq(10L));
        }

        @Test
        @DisplayName("틀린 코드 입력 시 false 반환 + 실패 카운트 1 증가")
        void wrongCode_firstFail_incrementsFailCount() {
            when(redisService.getStringData(FAIL_KEY)).thenReturn(null); // 첫 실패
            when(redisService.getStringData(AUTH_KEY)).thenReturn(CORRECT_CODE);

            boolean result = verifyService.verifiedAuthCode(TEST_EMAIL, WRONG_CODE);

            assertThat(result).isFalse();
            verify(redisService).saveStringData(eq(FAIL_KEY), eq("1"), eq('m'), eq(5L));
            verify(redisService, never()).deleteData(AUTH_KEY);
        }

        @Test
        @DisplayName("틀린 코드 4회 누적 시 false 반환 + 실패 카운트 4로 갱신 (코드 유지)")
        void wrongCode_fourthFail_incrementsTo4() {
            when(redisService.getStringData(FAIL_KEY)).thenReturn("3"); // 기존 3회
            when(redisService.getStringData(AUTH_KEY)).thenReturn(CORRECT_CODE);

            boolean result = verifyService.verifiedAuthCode(TEST_EMAIL, WRONG_CODE);

            assertThat(result).isFalse();
            verify(redisService).saveStringData(eq(FAIL_KEY), eq("4"), eq('m'), eq(5L));
            verify(redisService, never()).deleteData(AUTH_KEY);
        }

        @Test
        @DisplayName("틀린 코드 5회 도달 시 false 반환 + 코드·실패키 즉시 무효화 (Brute Force 차단)")
        void wrongCode_fifthFail_invalidatesCode() {
            when(redisService.getStringData(FAIL_KEY)).thenReturn("4"); // 기존 4회 → 5회 도달
            when(redisService.getStringData(AUTH_KEY)).thenReturn(CORRECT_CODE);

            boolean result = verifyService.verifiedAuthCode(TEST_EMAIL, WRONG_CODE);

            assertThat(result).isFalse();
            // 코드와 실패 카운트 즉시 삭제
            verify(redisService).deleteData(AUTH_KEY);
            verify(redisService).deleteData(FAIL_KEY);
            // 실패 카운트 재저장 안 함
            verify(redisService, never()).saveStringData(eq(FAIL_KEY), any(), anyChar(), anyLong());
            // 인증 완료 플래그 저장 안 함
            verify(redisService, never()).saveStringData(eq(VERIFIED_KEY), any(), anyChar(), anyLong());
        }

        @Test
        @DisplayName("Redis에 인증코드 없을 때 (만료/무효화) false 반환 — 실패 카운트 증가 안 함")
        void noCodeInRedis_returnsFalseWithoutIncrementingFailCount() {
            when(redisService.getStringData(FAIL_KEY)).thenReturn(null);
            when(redisService.getStringData(AUTH_KEY)).thenReturn(null); // 코드 없음

            boolean result = verifyService.verifiedAuthCode(TEST_EMAIL, WRONG_CODE);

            assertThat(result).isFalse();
            verify(redisService, never()).saveStringData(eq(FAIL_KEY), any(), anyChar(), anyLong());
            verify(redisService, never()).deleteData(any());
        }

        @Test
        @DisplayName("실패 후 올바른 코드 입력 시 실패 카운트도 함께 삭제")
        void afterFail_correctCode_clearsFailCount() {
            when(redisService.getStringData(FAIL_KEY)).thenReturn("2"); // 기존 2회 실패
            when(redisService.getStringData(AUTH_KEY)).thenReturn(CORRECT_CODE);

            boolean result = verifyService.verifiedAuthCode(TEST_EMAIL, CORRECT_CODE);

            assertThat(result).isTrue();
            verify(redisService).deleteData(AUTH_KEY);
            verify(redisService).deleteData(FAIL_KEY); // 실패 카운트도 삭제
        }
    }
}
