package com.godLife.project.controller.v1;

import com.godLife.project.dto.model.user.UserDTO;
import com.godLife.project.dto.query.user.GetNameNEmail;
import com.godLife.project.dto.request.myPage.GetUserPwRequestDTO;
import com.godLife.project.dto.response.user.UserProfileResponseDTO;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.service.impl.redis.RedisService;
import com.godLife.project.service.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  private final RedisService redisService;

  private final GlobalExceptionHandler handler;

  // 회원가입
  @Operation(summary = "회원가입 API", description = "유효성 검사 후 모두 통과시 정보 Insert")
  @PostMapping("/join")
  public ResponseEntity<Map<String, String>> join (@Valid @RequestBody UserDTO joinUserDTO, BindingResult result) {
    // System.out.println(joinUserDTO);
    // 유효성 검사
    if (result.hasErrors()) {
      Map<String, String> errors = new HashMap<>();
      result.getFieldErrors().forEach(fieldError -> {
        errors.put(fieldError.getField(), fieldError.getDefaultMessage());
      });

      return ResponseEntity.badRequest().body(errors);
    }
    else {
      // 데이터 삽입
      String successMessage = userService.insertUser(joinUserDTO);
      // 삽입 완료시 완료 메세지
      if ("Success".equals(successMessage)) {
        Map<String, String> success = new HashMap<>();
        success.put("message", "회원가입 완료");
        return ResponseEntity.ok(success);
      }
      // 삽입 에러시 에러 메세지
      else {
        Map<String, String> error = new HashMap<>();
        error.put("message",successMessage);
        return ResponseEntity.badRequest().body(error);
      }
    }
  }

  // 아이디 중복 체크
  @Operation(summary = "회원가입_아이디 체크 API", description = "중복 아이디 조회")
  @GetMapping("/checkId/{userId}")
  public ResponseEntity<Boolean> checkUserIdExist(@PathVariable String userId) {
    //System.out.println(userId + " : 아이디 체크 요청");
    Boolean isAvailable = userService.checkUserIdExist(userId);
    return ResponseEntity.ok(isAvailable);
  }

  // 아이디 찾기 (마스킹) — GET → POST 변경: 이름/이메일이 URL 로그에 노출되지 않도록
  @PostMapping("/find/userId")
  public ResponseEntity<Map<String, Object>> findUserId(@Valid @RequestBody GetNameNEmail request,
                                                        BindingResult valid) {
    if (valid.hasErrors()) {
      return ResponseEntity.badRequest().body(handler.getValidationErrors(valid));
    }

    String result = userService.FindUserIdByNameNEmail(request, true);

    if (result == null || result.isBlank()) {
      return ResponseEntity.status(404).body(handler.createResponse(404, "아이디가 없습니다."));
    }
    return ResponseEntity.ok().body(handler.createResponse(200, result));
  }

  // 아이디 찾기 (마스킹 해제) — GET → POST 변경 + TOCTOU 방어를 위한 원자적 인증 플래그 처리
  @PostMapping("/find/userId/noMask")
  public ResponseEntity<Map<String, Object>> noMaskingUserId(@Valid @RequestBody GetNameNEmail request,
                                                             BindingResult valid) {
    if (valid.hasErrors()) {
      return ResponseEntity.badRequest().body(handler.getValidationErrors(valid));
    }

    // 이메일 인증 플래그를 원자적으로 조회+삭제 (GETDEL) — Race Condition 방지
    String key = "EMAIL_VERIFIED: " + request.getUserEmail();
    String verified = redisService.getAndDeleteStringData(key);

    if (!"true".equals(verified)) {
      return ResponseEntity.status(handler.getHttpStatus(412))
          .body(handler.createResponse(412, "이메일 인증이 필요합니다."));
    }

    String result = userService.FindUserIdByNameNEmail(request, false);

    if (result == null || result.isBlank()) {
      return ResponseEntity.status(404).body(handler.createResponse(404, "아이디가 없습니다."));
    }
    return ResponseEntity.ok().body(handler.createResponse(200, result));
  }

  // 비밀번호 초기화 — Path Variable 이메일 제거: URL 로그에 이메일이 노출되지 않도록 Body로 이동
  //                    TOCTOU 방어: 인증 플래그를 원자적으로 조회+삭제
  //                    500 시 인증 플래그 재삭제 방지: getAndDelete로 사전에 처리
  @PatchMapping("/find/userPw")
  public ResponseEntity<Map<String, Object>> findUserPw(@Valid @RequestBody GetUserPwRequestDTO request,
                                                        BindingResult valid) {
    if (valid.hasErrors()) {
      return ResponseEntity.badRequest().body(handler.getValidationErrors(valid));
    }

    // 이메일 인증 플래그를 원자적으로 조회+삭제 (GETDEL) — Race Condition 방지
    String key = "EMAIL_VERIFIED: " + request.getUserEmail();
    String verified = redisService.getAndDeleteStringData(key);

    if (!"true".equals(verified)) {
      return ResponseEntity.status(handler.getHttpStatus(412))
          .body(handler.createResponse(412, "이메일 인증이 필요합니다."));
    }

    int result = userService.FindUserPw(request, request.getUserEmail());

    // 응답 메세지 세팅
    String msg = switch (result) {
      case 200 -> "비밀번호 수정 완료";
      case 400 -> "비밀번호 확인 필드의 값이 누락되었습니다.";
      case 404 -> "탈퇴했거나, 존재하지 않는 유저입니다.";
      case 422 -> "비밀번호가 일치하지 않습니다.";
      case 500 -> "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> "알 수 없는 오류가 발생했습니다.";
    };

    return ResponseEntity.status(handler.getHttpStatus(result)).body(handler.createResponse(result, msg));
  }

  // 프로필 조회
  @Operation(summary = "유저 프로필 조회 API", description = "로그인 후 유저의 프로필 데이터 조회")
  @GetMapping("/auth/profile")
  public ResponseEntity<Map<String, Object>> getUserProfile(@RequestHeader("Authorization") String authHeader) {
    String userId = handler.getUserNameFromToken(authHeader);
    UserProfileResponseDTO result = userService.getUserProfile(userId);

    if (result == null) {
      return ResponseEntity.status(404).body(handler.createResponse(404, "유저 정보가 없습니다."));
    }
    return ResponseEntity.ok(handler.createResponse(200, result));
  }

}
