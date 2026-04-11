package com.godLife.project.dto.request.verify;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GetEmailRequestDTO {
  // @CheckUserEmail 제거: 이메일 존재 여부를 응답 코드로 구별하지 않음 (이메일 열거 공격 방지)
  // 실제 이메일 존재 여부 확인 및 발송 처리는 서비스 계층에서 처리
  @Schema(description = "유저 이메일", example = "hong@example.com")
  @NotBlank(message = "{joinUser.userEmail.notBlank}")
  @Email(message = "{joinUser.userEmail.email}")
  private String userEmail;
}
