package com.godLife.project.dto.security;

import lombok.Data;

import java.util.Date;

@Data
public class RefreshDTO {

  private int tokenIdx;

  private String username;
  private String refresh;
  private Date expiration;
}
