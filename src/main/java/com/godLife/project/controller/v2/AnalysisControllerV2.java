package com.godLife.project.controller.v2;

import com.godLife.project.dto.response.plan.v2.TodayStatsDTO;
import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.service.interfaces.v2.ListServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/analysis")
public class AnalysisControllerV2 {

  private final ListServiceV2 listServiceV2;

  @GetMapping("/auth/todayStats")
  public ResponseEntity<?> getTodayStats(
          @AuthenticationPrincipal CustomUserDetails user) {
    try {
      int userIdx = user.getUserIdx();
      TodayStatsDTO stats = listServiceV2.getTodayStats(userIdx);
      return ResponseEntity.ok().body(ApiResponse.of(200, "통계 조회 성공", stats));
    } catch (Exception e) {
      String msg = "통계 조회 중 오류가 발생했습니다.";
      log.error("e: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.of(500, msg));
    }
  }

  /* -----------------------------------------// 함수 구현 //------------------------------------------------------- */

  /* --------------------------------------------------------------------------------------------------------------- */
}
