package com.godLife.project.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.service.impl.redis.RedisService;
import com.godLife.project.service.interfaces.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController - 아이디 찾기 / 비밀번호 초기화 보안 테스트")
class UserControllerAccountRecoveryTest {

    @Mock private UserService userService;
    @Mock private RedisService redisService;
    @Mock private GlobalExceptionHandler handler;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    // ──────────────────────────────────────────────────────────────────────
    // POST /api/v1/user/find/userId — 아이디 찾기 (마스킹)
    // ──────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /find/userId — 아이디 찾기 (마스킹)")
    class FindUserIdTest {

        @Test
        @DisplayName("정상 요청 시 200 + 마스킹된 아이디 반환")
        void validRequest_returns200WithMaskedId() throws Exception {
            Map<String, Object> okBody = Map.of("status", 200, "data", "test***");
            when(userService.FindUserIdByNameNEmail(any(), eq(true))).thenReturn("test***");
            when(handler.createResponse(eq(200), eq("test***"))).thenReturn(okBody);

            mockMvc.perform(post("/api/v1/user/find/userId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        { "userName": "홍길동", "userEmail": "test@example.com" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("test***"));
        }

        @Test
        @DisplayName("일치하는 아이디 없으면 404 반환")
        void noMatch_returns404() throws Exception {
            Map<String, Object> notFoundBody = Map.of("status", 404, "message", "아이디가 없습니다.");
            when(userService.FindUserIdByNameNEmail(any(), eq(true))).thenReturn(null);
            when(handler.createResponse(eq(404), eq("아이디가 없습니다."))).thenReturn(notFoundBody);

            mockMvc.perform(post("/api/v1/user/find/userId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        { "userName": "홍길동", "userEmail": "test@example.com" }
                        """))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET 메서드로 요청 시 405 Method Not Allowed — 민감정보가 URL에 노출되지 않음")
        void getMethodNotAllowed() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/api/v1/user/find/userId")
                    .param("userName", "홍길동")
                    .param("userEmail", "test@example.com"))
                .andExpect(status().isMethodNotAllowed());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // POST /api/v1/user/find/userId/noMask — 아이디 찾기 (마스킹 해제)
    // ──────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /find/userId/noMask — TOCTOU 방어 (GETDEL 원자적 처리)")
    class NoMaskUserIdTest {

        private static final String EMAIL = "test@example.com";
        private static final String REDIS_KEY = "EMAIL_VERIFIED: " + EMAIL;

        @Test
        @DisplayName("인증 플래그 없으면 412 반환 — 인증 없이 아이디 조회 불가")
        void noVerifiedFlag_returns412() throws Exception {
            Map<String, Object> precondBody = Map.of("status", 412, "message", "이메일 인증이 필요합니다.");
            when(redisService.getAndDeleteStringData(REDIS_KEY)).thenReturn(null);
            when(handler.getHttpStatus(412)).thenReturn(HttpStatus.PRECONDITION_FAILED);
            when(handler.createResponse(eq(412), eq("이메일 인증이 필요합니다."))).thenReturn(precondBody);

            mockMvc.perform(post("/api/v1/user/find/userId/noMask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        { "userName": "홍길동", "userEmail": "test@example.com" }
                        """))
                .andExpect(status().isPreconditionFailed());
        }

        @Test
        @DisplayName("인증 플래그 값이 'true'가 아니면 412 반환 — 조작된 플래그 차단")
        void wrongVerifiedFlagValue_returns412() throws Exception {
            Map<String, Object> precondBody = Map.of("status", 412, "message", "이메일 인증이 필요합니다.");
            when(redisService.getAndDeleteStringData(REDIS_KEY)).thenReturn("yes"); // "true" 아닌 값
            when(handler.getHttpStatus(412)).thenReturn(HttpStatus.PRECONDITION_FAILED);
            when(handler.createResponse(eq(412), eq("이메일 인증이 필요합니다."))).thenReturn(precondBody);

            mockMvc.perform(post("/api/v1/user/find/userId/noMask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        { "userName": "홍길동", "userEmail": "test@example.com" }
                        """))
                .andExpect(status().isPreconditionFailed());
        }

        @Test
        @DisplayName("유효한 인증 플래그 + 아이디 존재 시 200 반환")
        void validFlag_returns200WithUserId() throws Exception {
            Map<String, Object> okBody = Map.of("status", 200, "data", "testUser");
            when(redisService.getAndDeleteStringData(REDIS_KEY)).thenReturn("true");
            when(userService.FindUserIdByNameNEmail(any(), eq(false))).thenReturn("testUser");
            when(handler.createResponse(eq(200), eq("testUser"))).thenReturn(okBody);

            mockMvc.perform(post("/api/v1/user/find/userId/noMask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        { "userName": "홍길동", "userEmail": "test@example.com" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("testUser"));
        }

        @Test
        @DisplayName("인증 플래그는 GETDEL로 단 1회만 소비됨 — 재사용 불가 (TOCTOU 차단)")
        void verifiedFlagConsumedOnce_cannotReuse() throws Exception {
            Map<String, Object> precondBody = Map.of("status", 412, "message", "이메일 인증이 필요합니다.");
            // 첫 번째 호출: "true" 반환 (소비됨)
            // 두 번째 호출: null 반환 (이미 삭제됨)
            when(redisService.getAndDeleteStringData(REDIS_KEY))
                .thenReturn("true")
                .thenReturn(null);
            when(userService.FindUserIdByNameNEmail(any(), eq(false))).thenReturn("testUser");
            when(handler.createResponse(eq(200), eq("testUser")))
                .thenReturn(Map.of("status", 200, "data", "testUser"));
            when(handler.getHttpStatus(412)).thenReturn(HttpStatus.PRECONDITION_FAILED);
            when(handler.createResponse(eq(412), eq("이메일 인증이 필요합니다."))).thenReturn(precondBody);

            String body = """
                { "userName": "홍길동", "userEmail": "test@example.com" }
                """;

            // 첫 번째 요청: 성공
            mockMvc.perform(post("/api/v1/user/find/userId/noMask")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

            // 두 번째 요청: 플래그가 이미 삭제되어 412
            mockMvc.perform(post("/api/v1/user/find/userId/noMask")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPreconditionFailed());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // PATCH /api/v1/user/find/userPw — 비밀번호 초기화
    // ──────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /find/userPw — 비밀번호 초기화 보안 테스트")
    class FindUserPwTest {

        private static final String EMAIL = "test@example.com";
        private static final String REDIS_KEY = "EMAIL_VERIFIED: " + EMAIL;

        @Test
        @DisplayName("인증 플래그 없으면 412 반환 — 인증 없이 비밀번호 변경 불가")
        void noVerifiedFlag_returns412() throws Exception {
            Map<String, Object> precondBody = Map.of("status", 412, "message", "이메일 인증이 필요합니다.");
            when(redisService.getAndDeleteStringData(REDIS_KEY)).thenReturn(null);
            when(handler.getHttpStatus(412)).thenReturn(HttpStatus.PRECONDITION_FAILED);
            when(handler.createResponse(eq(412), eq("이메일 인증이 필요합니다."))).thenReturn(precondBody);

            mockMvc.perform(patch("/api/v1/user/find/userPw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "userEmail": "test@example.com",
                          "userPw": "NewPass1234!",
                          "userPwConfirm": "NewPass1234!"
                        }
                        """))
                .andExpect(status().isPreconditionFailed());

            verify(userService, never()).FindUserPw(any(), any());
        }

        @Test
        @DisplayName("유효한 인증 플래그 + 정상 요청 시 200 반환 + 서비스 호출")
        void validFlag_returns200() throws Exception {
            Map<String, Object> okBody = Map.of("status", 200, "message", "비밀번호 수정 완료");
            when(redisService.getAndDeleteStringData(REDIS_KEY)).thenReturn("true");
            when(userService.FindUserPw(any(), eq(EMAIL))).thenReturn(200);
            when(handler.getHttpStatus(200)).thenReturn(HttpStatus.OK);
            when(handler.createResponse(eq(200), eq("비밀번호 수정 완료"))).thenReturn(okBody);

            mockMvc.perform(patch("/api/v1/user/find/userPw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "userEmail": "test@example.com",
                          "userPw": "NewPass1234!",
                          "userPwConfirm": "NewPass1234!"
                        }
                        """))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("이메일이 URL Path가 아닌 Body에서 처리됨 — URL 민감정보 노출 없음")
        void emailInBodyNotPath_noEmailInUrl() throws Exception {
            // /find/userPw/{userEmail} 형식으로 요청하면 404 (경로 없음)
            mockMvc.perform(patch("/api/v1/user/find/userPw/test@example.com")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("서비스에서 비밀번호 불일치(422) 반환 시 올바른 HTTP 상태 반환")
        void servicePwMismatch_returns422() throws Exception {
            Map<String, Object> body422 = Map.of("status", 422, "message", "비밀번호가 일치하지 않습니다.");
            when(redisService.getAndDeleteStringData(REDIS_KEY)).thenReturn("true");
            when(userService.FindUserPw(any(), eq(EMAIL))).thenReturn(422);
            when(handler.getHttpStatus(422)).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
            when(handler.createResponse(eq(422), eq("비밀번호가 일치하지 않습니다."))).thenReturn(body422);

            mockMvc.perform(patch("/api/v1/user/find/userPw")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "userEmail": "test@example.com",
                          "userPw": "NewPass1234!",
                          "userPwConfirm": "NewPass1234!"
                        }
                        """))
                .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("인증 플래그는 GETDEL로 단 1회만 소비됨 — 비밀번호 초기화 재사용 공격 차단")
        void verifiedFlagConsumedOnce_preventReuse() throws Exception {
            Map<String, Object> precondBody = Map.of("status", 412, "message", "이메일 인증이 필요합니다.");
            when(redisService.getAndDeleteStringData(REDIS_KEY))
                .thenReturn("true")
                .thenReturn(null);
            when(userService.FindUserPw(any(), eq(EMAIL))).thenReturn(200);
            when(handler.getHttpStatus(200)).thenReturn(HttpStatus.OK);
            when(handler.createResponse(eq(200), eq("비밀번호 수정 완료")))
                .thenReturn(Map.of("status", 200, "message", "비밀번호 수정 완료"));
            when(handler.getHttpStatus(412)).thenReturn(HttpStatus.PRECONDITION_FAILED);
            when(handler.createResponse(eq(412), eq("이메일 인증이 필요합니다."))).thenReturn(precondBody);

            String requestBody = """
                {
                  "userEmail": "test@example.com",
                  "userPw": "NewPass1234!",
                  "userPwConfirm": "NewPass1234!"
                }
                """;

            // 첫 번째 요청: 성공
            mockMvc.perform(patch("/api/v1/user/find/userPw")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk());

            // 두 번째 요청: 플래그 소진 → 412
            mockMvc.perform(patch("/api/v1/user/find/userPw")
                    .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isPreconditionFailed());
        }
    }
}
