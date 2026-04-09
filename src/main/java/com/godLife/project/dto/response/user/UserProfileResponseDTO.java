package com.godLife.project.dto.response.user;

import lombok.Data;

@Data
public class UserProfileResponseDTO {
  private String userNick;
  private String nickTag;
  private int jobIdx;
  private int targetIdx;
  private int combo;
  private double userExp;
  private int userLv;
}
