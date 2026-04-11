package com.godLife.project.dto.query.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GetNameNEmail {
  @NotBlank(message = "{joinUser.userName.notBlank}")
  @Size(min=3, max=15, message = "{joinUser.userName.size}")
  private String userName;

  // @CheckUserEmail 제거: 이메일 존재 여부를 400 vs 200/404로 구별하면 이메일 열거 공격에 악용될 수 있음
  // 미등록 이메일은 DB 조회 결과가 null → 기존과 동일하게 404 반환
  @Email(message = "{joinUser.userEmail.email}")
  @NotBlank(message = "{joinUser.userEmail.notBlank}")
  private String userEmail;
}
