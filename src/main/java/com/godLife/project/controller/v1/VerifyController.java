package com.godLife.project.controller.v1;

import com.godLife.project.dto.request.verify.GetEmailRequestDTO;
import com.godLife.project.dto.request.verify.VerifyRequestDTO;
import com.godLife.project.dto.request.myPage.ModifyEmailRequestDTO;
import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.service.interfaces.VerifyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/verify")
public class VerifyController {

  private final VerifyService verifyService;

  // 루틴 인증 엔드포인트
  @PostMapping("/auth/routine")
  public ResponseEntity<?> verifyRoutine(@AuthenticationPrincipal CustomUserDetails user,
                                                           @RequestBody VerifyRequestDTO verifyRequestDTO) {
    // userIdx 조회
    int userIdx = user.getUserIdx();
    verifyRequestDTO.setUserIdx(userIdx);

    int result = verifyService.verifyActivity(verifyRequestDTO);

    // 응답 메세지 세팅
    String msg = "";
    switch (result) {
      case 200 -> msg = "활동 인증이 정상적으로 처리되었습니다.";
      case 403 -> msg = "작성자가 아닙니다. 재로그인 해주세요.";
      case 404 -> msg = "루틴 혹은 활동이 존재하지 않거나, 삭제 처리된 상태입니다.";
      case 409 -> msg = "이미 인증한 활동입니다.";
      case 410 -> msg = "회원 탈퇴한 계정입니다.";
      case 412 -> msg = "활성화 된 루틴이 아닙니다. 루틴 활성화 후 실행해 주세요.";
      case 500 -> msg = "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> msg = "알 수 없는 오류가 발생했습니다.";
    }

    // 응답 메시지 설정
    return ResponseEntity.status(result).body(ApiResponse.of(result, msg));
  }


  // 이메일 인증 번호 요청 엔드포인트 (가입/수정)
  @PostMapping("/emails/send/verification-requests")
  public ResponseEntity<?> sendAuthCode(@Valid @RequestBody ModifyEmailRequestDTO emailRequestDTO,
                                             BindingResult valid) {
    if (valid.hasErrors()) {
      Map<String, String> errors = new java.util.LinkedHashMap<>();
      valid.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      return ResponseEntity.badRequest().body((Map) errors);
    }
    String email = emailRequestDTO.getUserEmail();

    try {
      verifyService.sendCodeToEmail(email);
    } catch (IllegalStateException e) {
      // 1분 내 재발송 요청 — 429 Too Many Requests
      return ResponseEntity.status(429).body(ApiResponse.of(429, e.getMessage()));
    }

    return ResponseEntity.ok().build();
  }

  // 이메일 인증 번호 요청 엔드포인트 (단순인증 — 아이디 찾기 / 비밀번호 초기화)
  @PostMapping("/emails/send/just/verification-requests")
  public ResponseEntity<?> sendJustAuthCode(@Valid @RequestBody GetEmailRequestDTO emailRequestDTO,
                                                          BindingResult valid) {
    if (valid.hasErrors()) {
      Map<String, String> errors = new java.util.LinkedHashMap<>();
      valid.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      return ResponseEntity.badRequest().body((Map) errors);
    }
    String email = emailRequestDTO.getUserEmail();

    try {
      verifyService.sendCodeToEmailForFindAccount(email);
    } catch (IllegalStateException e) {
      // 1분 내 재발송 요청 — 429 Too Many Requests
      return ResponseEntity.status(429).body(ApiResponse.of(429, e.getMessage()));
    }

    return ResponseEntity.ok().build();
  }

  // 이메일 인증 번호 검증 엔드포인트 (가입/수정)
  @PostMapping("/emails/verifications")
  public ResponseEntity<?> verificationEmail(@Valid @RequestBody ModifyEmailRequestDTO emailRequestDTO,
                                                                BindingResult valid,
                                                                @RequestParam("code") String code) {
    if (valid.hasErrors()) {
      Map<String, String> errors = new java.util.LinkedHashMap<>();
      valid.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      return ResponseEntity.badRequest().body((Map) errors);
    }
    String email = emailRequestDTO.getUserEmail();

    boolean result = verifyService.verifiedAuthCode(email, code);

    return ResponseEntity.ok(ApiResponse.of(200, "이메일 인증 결과", Map.of("verified", result)));
  }

  // 이메일 인증 번호 검증 엔드포인트 (단순인증)
  @PostMapping("/emails/just/verifications")
  public ResponseEntity<?> justVerificationEmail(@Valid @RequestBody GetEmailRequestDTO emailRequestDTO,
                                                               BindingResult valid,
                                                               @RequestParam("code") String code) {
    if (valid.hasErrors()) {
      Map<String, String> errors = new java.util.LinkedHashMap<>();
      valid.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      return ResponseEntity.badRequest().body((Map) errors);
    }
    String email = emailRequestDTO.getUserEmail();

    boolean result = verifyService.verifiedAuthCode(email, code);

    return ResponseEntity.ok(ApiResponse.of(200, "이메일 인증 결과", Map.of("verified", result)));
  }
}
