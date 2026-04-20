package com.godLife.project.controller.v1;

import com.godLife.project.dto.model.user.UserDTO;
import com.godLife.project.dto.query.user.GetNameNEmail;
import com.godLife.project.dto.request.myPage.GetUserPwRequestDTO;
import com.godLife.project.dto.response.user.UserProfileResponseDTO;
import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.service.impl.redis.RedisService;
import com.godLife.project.service.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

  // 회원가입
  @Operation(summary = "회원가입 API", description = "유효성 검사 후 모두 통과시 정보 Insert")
  @PostMapping("/join")
  public ResponseEntity<Map<String, String>> join (@Valid @RequestBody UserDTO joinUserDTO, BindingResult result) {
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

  // 아이디 찾기 (마스킹)
  @PostMapping("/find/userId")
  public ResponseEntity<?> findUserId(@Valid @RequestBody GetNameNEmail request,
                                                        BindingResult valid) {
    if (valid.hasErrors()) {
      Map<String, String> errors = new HashMap<>();
      valid.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      return ResponseEntity.badRequest().body((Map) errors);
    }

    String result = userService.FindUserIdByNameNEmail(request, true);

    if (result == null || result.isBlank()) {
      return ResponseEntity.status(404).body(ApiResponse.of(404, "아이디가 없습니다."));
    }
    return ResponseEntity.ok().body(ApiResponse.of(200, "아이디 찾기 성공", result));
  }

  // 아이디 찾기 (마스킹 해제)
  @PostMapping("/find/userId/noMask")
  public ResponseEntity<?> noMaskingUserId(@Valid @RequestBody GetNameNEmail request,
                                                             BindingResult valid) {
    if (valid.hasErrors()) {
      Map<String, String> errors = new HashMap<>();
      valid.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      return ResponseEntity.badRequest().body((Map) errors);
    }

    // 이메일 인증 플래그를 원자적으로 조회+삭제
    String key = "EMAIL_VERIFIED: " + request.getUserEmail();
    String verified = redisService.getAndDeleteStringData(key);

    if (!"true".equals(verified)) {
      return ResponseEntity.status(412)
          .body(ApiResponse.of(412, "이메일 인증이 필요합니다."));
    }

    String result = userService.FindUserIdByNameNEmail(request, false);

    if (result == null || result.isBlank()) {
      return ResponseEntity.status(404).body(ApiResponse.of(404, "아이디가 없습니다."));
    }
    return ResponseEntity.ok().body(ApiResponse.of(200, "아이디 찾기 성공", result));
  }

  // 비밀번호 초기화
  @PatchMapping("/find/userPw")
  public ResponseEntity<?> findUserPw(@Valid @RequestBody GetUserPwRequestDTO request,
                                                        BindingResult valid) {
    if (valid.hasErrors()) {
      Map<String, String> errors = new HashMap<>();
      valid.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      return ResponseEntity.badRequest().body((Map) errors);
    }

    // 이메일 인증 플래그를 원자적으로 조회+삭제
    String key = "EMAIL_VERIFIED: " + request.getUserEmail();
    String verified = redisService.getAndDeleteStringData(key);

    if (!"true".equals(verified)) {
      return ResponseEntity.status(412)
          .body(ApiResponse.of(412, "이메일 인증이 필요합니다."));
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

    return ResponseEntity.status(result).body(ApiResponse.of(result, msg));
  }

  // 프로필 조회
  @Operation(summary = "유저 프로필 조회 API", description = "로그인 후 유저의 프로필 데이터 조회")
  @GetMapping("/auth/profile")
  public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal CustomUserDetails user) {
    String userId = user.getUsername();
    UserProfileResponseDTO result = userService.getUserProfile(userId);

    if (result == null) {
      return ResponseEntity.status(404).body(ApiResponse.of(404, "유저 정보가 없습니다."));
    }
    return ResponseEntity.ok(ApiResponse.of(200, "유저 프로필 조회 성공", result));
  }

}
