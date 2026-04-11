package com.godLife.project.service.interfaces.v2;

import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface PlanServiceV2 {

  // 루틴과 활동 상세 조회 — PlanDetailDTO (boolean 플래그, 읽기 전용 응답)
  PlanDetailDTO detailRoutine(int planIdx, int isDeleted, HttpServletRequest request);
}
