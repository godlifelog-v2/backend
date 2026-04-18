package com.godLife.project.mapper.v2;

import com.godLife.project.dto.query.plan.v2.CustomPlanV2DTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ListMapperV2 {

  // (개인용) 전체 루틴 조회 — v2 필드(boolean isShared/isActive, List<String> repeatDays, color)
  List<CustomPlanV2DTO> getMyPlansByUserIdx(int userIdx);

  // 오늘 요일에 해당하는 활성 루틴 조회 (dayIdx: PLAN_REPEAT_DAYS.DAY_IDX)
  List<CustomPlanV2DTO> getTodayPlansByUserIdx(@Param("userIdx") int userIdx,
                                               @Param("dayIdx") int dayIdx);

  // 오늘 활성 루틴 총 수
  int getTodayPlanCount(@Param("userIdx") int userIdx,
                        @Param("dayIdx") int dayIdx);

  // 오늘 모든 activity가 verified인 루틴 수
  int getTodayCompletedPlanCount(@Param("userIdx") int userIdx,
                                 @Param("dayIdx") int dayIdx);
}
