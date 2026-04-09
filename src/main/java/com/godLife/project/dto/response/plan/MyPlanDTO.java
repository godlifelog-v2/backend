package com.godLife.project.dto.response.plan;

import com.godLife.project.dto.category.JobCateDTO;
import com.godLife.project.dto.category.JobEtcCateDTO;
import com.godLife.project.dto.category.TargetCateDTO;
import com.godLife.project.dto.model.plan.ActivityDTO;
import com.godLife.project.dto.model.common.FireDTO;
import com.godLife.project.dto.query.plan.CustomPlanDTO;
import lombok.Data;

import java.util.List;

@Data
public class MyPlanDTO {

  // 나의 루틴 정보
  private CustomPlanDTO myPlanInfos;

  // 나의 활동 정보
  private List<ActivityDTO> myActivities;

  // 나의 직업(기본) 정보
  private JobCateDTO jobDefaultInfos;
  // 나의 직업(직접입력) 정보
  private JobEtcCateDTO jobAddedInfos;

  // 나의 목표(관심사) 정보
  private TargetCateDTO targetInfos;

  // 나의 불꽃 정보
  private FireDTO fireInfos;
}
