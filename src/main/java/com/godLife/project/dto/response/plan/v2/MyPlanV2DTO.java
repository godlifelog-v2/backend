package com.godLife.project.dto.response.plan.v2;

import com.godLife.project.dto.category.JobCateDTO;
import com.godLife.project.dto.category.JobEtcCateDTO;
import com.godLife.project.dto.category.TargetCateDTO;
import com.godLife.project.dto.model.common.FireDTO;
import com.godLife.project.dto.response.plan.v2.ActivityV2DTO;
import com.godLife.project.dto.query.plan.v2.CustomPlanV2DTO;
import lombok.Data;

import java.util.List;

@Data
public class MyPlanV2DTO {

  private CustomPlanV2DTO planInfos;
  private List<ActivityV2DTO> activities;
  private JobCateDTO jobCateDTO;
  private JobEtcCateDTO jobEtcCateDTO;
  private TargetCateDTO targetCateDTO;
  private FireDTO fireInfo;
}
