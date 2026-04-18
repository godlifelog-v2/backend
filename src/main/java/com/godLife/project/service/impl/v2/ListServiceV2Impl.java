package com.godLife.project.service.impl.v2;

import com.godLife.project.dto.query.plan.v2.CustomPlanV2DTO;
import com.godLife.project.dto.response.plan.v2.MyPlanV2DTO;
import com.godLife.project.dto.response.plan.v2.TodayStatsDTO;
import com.godLife.project.enums.RepeatDay;
import com.godLife.project.mapper.ListMapper;
import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.mapper.VerifyMapper;
import com.godLife.project.mapper.v2.ListMapperV2;
import com.godLife.project.mapper.v2.PlanMapperV2;
import com.godLife.project.service.interfaces.CategoryService;
import com.godLife.project.service.interfaces.v2.ListServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListServiceV2Impl implements ListServiceV2 {

  private final ListMapperV2 listMapperV2;
  private final ListMapper listMapper;       // target/job idx 조회 (v1 공용)
  private final PlanMapper planMapper;
  private final PlanMapperV2 planMapperV2;   // v2 활동 목록 조회
  private final VerifyMapper verifyMapper;
  private final CategoryService categoryService;

  @Override
  @Transactional
  public List<MyPlanV2DTO> getMyPlansList(int userIdx) {
    try {
      if (!planMapper.getUserIsDeleted(userIdx).contains("N")) { return null; } // 탈퇴한 유저 조회 불가

      List<CustomPlanV2DTO> planDtos = listMapperV2.getMyPlansByUserIdx(userIdx);

      if (planDtos == null || planDtos.isEmpty()) {
        return new ArrayList<>();
      }

      return buildMyPlanList(planDtos);
    } catch (Exception e) {
      log.error("e: ", e);
      return null;
    }
  }

  @Override
  @Transactional
  public List<MyPlanV2DTO> getTodayPlansList(int userIdx) {
    try {
      if (!planMapper.getUserIsDeleted(userIdx).contains("N")) { return null; }

      int dayOfWeekIdx = getDayOfWeekIdx();
      List<CustomPlanV2DTO> planDtos = listMapperV2.getTodayPlansByUserIdx(userIdx, dayOfWeekIdx);

      if (planDtos == null || planDtos.isEmpty()) {
        return new ArrayList<>();
      }

      return buildMyPlanList(planDtos);
    } catch (Exception e) {
      log.error("e: ", e);
      return null;
    }
  }

  @Override
  public TodayStatsDTO getTodayStats(int userIdx) {
    int dayOfWeekIdx = getDayOfWeekIdx();
    int total = listMapperV2.getTodayPlanCount(userIdx, dayOfWeekIdx);
    int completed = listMapperV2.getTodayCompletedPlanCount(userIdx, dayOfWeekIdx);
    int combo = verifyMapper.getComboByUserIdx(userIdx);
    return new TodayStatsDTO(total, completed, combo);
  }

  /* -----------------------------------------// 함수 구현 //------------------------------------------------------- */

  /** 오늘 요일을 PLAN_REPEAT_DAYS.DAY_IDX (1=일 ~ 7=토) 형태로 반환 */
  private int getDayOfWeekIdx() {
    return RepeatDay.toDayIdx(LocalDate.now().getDayOfWeek());
  }

  /** CustomPlanV2DTO 리스트를 MyPlanV2DTO 리스트로 변환 (공통 조립 로직) */
  private List<MyPlanV2DTO> buildMyPlanList(List<CustomPlanV2DTO> planDtos) {
    List<MyPlanV2DTO> result = new ArrayList<>();
    int customJobIdx = categoryService.getIdxOfCustomJob();

    for (CustomPlanV2DTO planDTO : planDtos) {
      MyPlanV2DTO myPlanDTO = new MyPlanV2DTO();
      int planIdx = planDTO.getPlanIdx();
      int targetIdx = listMapper.getTargetIdxByPlanIdx(planIdx);
      int jobIdx = listMapper.getJobIdxByPlanIdx(planIdx);

      myPlanDTO.setPlanInfos(planDTO);
      myPlanDTO.setActivities(planMapperV2.getActivitiesByPlanIdx(planIdx));
      myPlanDTO.setTargetCateDTO(planMapper.getTargetCategoryByTargetIdx(targetIdx));
      myPlanDTO.setFireInfo(planMapper.detailFireByPlanIdx(planIdx));

      if (jobIdx == customJobIdx) {
        myPlanDTO.setJobEtcCateDTO(planMapper.getJobEtcInfoByPlanIdx(planIdx));
      } else {
        myPlanDTO.setJobCateDTO(planMapper.getJOBCategoryByJobIdx(jobIdx));
      }

      result.add(myPlanDTO);
    }
    return result;
  }
}
