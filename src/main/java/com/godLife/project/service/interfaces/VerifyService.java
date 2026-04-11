package com.godLife.project.service.interfaces;

import com.godLife.project.dto.request.verify.VerifyRequestDTO;

public interface VerifyService {
  // 활동 인증
  int verifyActivity(VerifyRequestDTO verifyRequestDTO);

  // 코드 생성 및 이메일 전송 (가입/수정용)
  void sendCodeToEmail(String toEmail);
  // 코드 생성 및 이메일 전송 (단순인증용 — 아이디 찾기/비밀번호 초기화, 이메일 열거 방지)
  void sendCodeToEmailForFindAccount(String toEmail);
  // 인증 코드 검증
  boolean verifiedAuthCode(String email, String authCode);
}
