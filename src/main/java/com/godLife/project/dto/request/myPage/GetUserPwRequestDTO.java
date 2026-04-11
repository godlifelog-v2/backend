package com.godLife.project.dto.request.myPage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GetUserPwRequestDTO {

  private int userIdx;

  // 비밀번호 초기화 엔드포인트에서 Path Variable 제거 후 Body로 이동 (URL 민감 정보 노출 방지)
  @Schema(description = "유저 이메일", example = "hong@example.com")
  @NotBlank(message = "{joinUser.userEmail.notBlank}")
  @Email(message = "{joinUser.userEmail.email}")
  private String userEmail;

  @Schema(description = "유저 비밀번호", example = "NewPass1234!")
  @NotBlank(message = "{joinUser.userPw.notBlank}")
  @Size(min = 4, max = 15, message = "{joinUser.userPw.size}")
  @Pattern(regexp = "^[a-zA-Z0-9!@#$%^*()_+\\-=\\[\\]{},.:?~]{8,20}$",
      message = "{joinUser.userPw.pattern}")
  private String userPw;

  private String originalPw;

  private String userPwConfirm;
}
