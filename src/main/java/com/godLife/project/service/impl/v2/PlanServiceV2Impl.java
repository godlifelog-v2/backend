package com.godLife.project.service.impl.v2;

import com.godLife.project.dto.model.plan.PlanDTO;
import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.service.interfaces.CategoryService;
import com.godLife.project.service.interfaces.v2.PlanServiceV2;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceV2Impl implements PlanServiceV2 {

  private final PlanMapper planMapper;
  private final CategoryService categoryService;
  private final GlobalExceptionHandler handler;

  // 루틴 상세 보기 로직 — PlanDetailDTO 반환
  @Override
  @Transactional
  public PlanDetailDTO detailRoutine(int planIdx, int isDeleted, HttpServletRequest request) {
    // 루틴 완료 처리
    planMapper.updateCompleteByPlanIdx(planIdx);

    // 루틴 조회
    PlanDTO planDTO = planMapper.detailPlanByPlanIdx(planIdx, isDeleted);

    if (planDTO == null) return null;

    String authHeader = request.getHeader("Authorization");
    boolean isPrivate = planDTO.getIsShared() == 0; // true: 비공개 루틴
    boolean existAuth = authHeader != null && authHeader.startsWith("Bearer ");

    if (existAuth) {
      int userIdx = handler.getUserIdxFromToken(authHeader); // 요청자 userIdx 조회

      if (userIdx == planDTO.getUserIdx()) { // 작성자 본인이라면
        planDTO.setIsWriter(1);
      }

      if (isPrivate && planDTO.getIsWriter() == 0) { // 비공개인데 작성자가 아닌 경우
        return null;
      }
    }

    if (!existAuth && isPrivate) { // 비공개인데 토큰도 없는 경우
      return null;
    }

    // 연관 데이터 조회
    planDTO.setActivities(planMapper.detailActivityByPlanIdx(planIdx));
    planDTO.setTargetCateDTO(planMapper.getTargetCategoryByTargetIdx(planDTO.getTargetIdx()));
    planDTO.setFireInfo(planMapper.detailFireByPlanIdx(planIdx));
    planDTO.setVerifyCount(planMapper.getVerifyCountByPlanIdx(planIdx));

    int customJobIdx = categoryService.getIdxOfCustomJob();
    if (planDTO.getJobIdx() == customJobIdx) {
      planDTO.setJobEtcCateDTO(planMapper.getJobEtcInfoByPlanIdx(planIdx));
    } else {
      planDTO.setJobCateDTO(planMapper.getJOBCategoryByJobIdx(planDTO.getJobIdx()));
    }

    // PlanDTO(int 플래그) → PlanDetailDTO(boolean 플래그) 변환
    return toPlanDetailDTO(planDTO);
  }

  /** PlanDTO → PlanDetailDTO 변환 (int 플래그 → boolean) */
  private PlanDetailDTO toPlanDetailDTO(PlanDTO planDTO) {
    PlanDetailDTO detail = new PlanDetailDTO();
    detail.setPlanIdx(planDTO.getPlanIdx());
    detail.setPlanTitle(planDTO.getPlanTitle());
    detail.setEndTo(planDTO.getEndTo());
    detail.setRepeatDays(planDTO.getRepeatDays());
    detail.setPlanImp(planDTO.getPlanImp());
    detail.setCertExp(planDTO.getCertExp());
    detail.setVerifyCount(planDTO.getVerifyCount());
    detail.setViewCount(planDTO.getViewCount());
    detail.setLikeCount(planDTO.getLikeCount());
    detail.setForkCount(planDTO.getForkCount());
    detail.setIsShared(planDTO.getIsShared() == 1);
    detail.setIsActive(planDTO.getIsActive() == 1);
    detail.setIsCompleted(planDTO.getIsCompleted() == 1);
    detail.setIsWriter(planDTO.getIsWriter() == 1);
    detail.setFireState(planDTO.isFireState());
    detail.setForked(planDTO.isForked());
    detail.setPlanSubDate(planDTO.getPlanSubDate());
    detail.setPlanSubStart(planDTO.getPlanSubStart());
    detail.setPlanSubEnd(planDTO.getPlanSubEnd());
    detail.setForkIdx(planDTO.getForkIdx());
    detail.setForkTitle(planDTO.getForkTitle());
    detail.setReview(planDTO.getReview());
    detail.setActivities(planDTO.getActivities());
    detail.setJobCateDTO(planDTO.getJobCateDTO());
    detail.setJobEtcCateDTO(planDTO.getJobEtcCateDTO());
    detail.setTargetCateDTO(planDTO.getTargetCateDTO());
    detail.setFireInfo(planDTO.getFireInfo());
    return detail;
  }
}
