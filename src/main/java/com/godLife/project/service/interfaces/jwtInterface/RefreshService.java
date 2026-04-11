package com.godLife.project.service.interfaces.jwtInterface;

public interface RefreshService {

  // 리프레쉬 토큰 유무 확인
  Boolean existsByRefresh(String refresh);

  // 리프레쉬 토큰 삭제 (토큰 값으로)
  void deleteByRefresh(String refresh);

  // 리프레쉬 토큰 전체 삭제 (유저 ID로 — 비밀번호 변경 시 기존 세션 무효화)
  void deleteByUsername(String username);

  // 리프레쉬 토큰 등록
  void addRefreshToken(String username, String refresh, Long expiredMs);

  void deleteAdminStatusByRedis(String userId);
}
