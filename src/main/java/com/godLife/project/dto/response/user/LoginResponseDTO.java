package com.godLife.project.dto.response.user;

import lombok.Data;

@Data
public class LoginResponseDTO {
  private String userNick;
  private String nickTag;
  private boolean roleStatus;
}
