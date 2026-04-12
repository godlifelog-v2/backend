package com.godLife.project.service.interfaces.v2;

import com.godLife.project.dto.response.plan.v2.MyPlanV2DTO;
import com.godLife.project.dto.response.plan.v2.TodayStatsDTO;

import java.util.List;

public interface ListServiceV2 {

  // 나의 진행중인 루틴 리스트 조회 (v2 응답 스키마)
  List<MyPlanV2DTO> getMyPlansList(int userIdx);

  // 오늘의 루틴 리스트 조회 (오늘 요일 기준 필터)
  List<MyPlanV2DTO> getTodayPlansList(int userIdx);

  // 오늘 요약 통계 조회
  TodayStatsDTO getTodayStats(int userIdx);
}
