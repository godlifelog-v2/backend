package com.godLife.project.service.impl.v2;

import com.godLife.project.dto.category.JobEtcCateDTO;
import com.godLife.project.dto.model.plan.PlanDTO;
import com.godLife.project.dto.request.plan.v2.*;
import com.godLife.project.dto.response.plan.v2.ActivityV2DTO;
import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import com.godLife.project.dto.response.plan.v2.PlanExtraInfoDTO;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.mapper.dto.PlanDetailMapper;
import com.godLife.project.mapper.v2.PlanMapperV2;
import com.godLife.project.service.interfaces.CategoryService;
import com.godLife.project.service.interfaces.v2.PlanServiceV2;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceV2Impl implements PlanServiceV2 {

    private final PlanMapper planMapper;
    private final PlanMapperV2 planMapperV2;
    private final PlanDetailMapper planDetailMapper;
    private final CategoryService categoryService;
    private final GlobalExceptionHandler handler;

    // ========================= 공통 가드 메서드 =========================

    private boolean isUserDeleted(int userIdx) {
        String deleted = planMapper.getUserIsDeleted(userIdx);
        return deleted == null || !deleted.contains("N");
    }

    private boolean planNotFound(int planIdx) {
        return !planMapper.checkPlanByPlanIdx(planIdx, 0);
    }

    private boolean notOwner(int planIdx, int userIdx) {
        return planMapper.getUserIdxByPlanIdx(planIdx) != userIdx;
    }

    private boolean activityNotFound(int planIdx, int activityIdx) {
        return !planMapper.checkActByActivityIdx(planIdx, activityIdx);
    }

    // ========================= 루틴 상세 조회 =========================

    @Override
    @Transactional
    public PlanDetailDTO detailRoutine(int planIdx, int isDeleted, HttpServletRequest request) {
        planMapper.updateCompleteByPlanIdx(planIdx);

        PlanDTO planDTO = planMapper.detailPlanByPlanIdx(planIdx, isDeleted);
        if (planDTO == null) return null;

        String authHeader = request.getHeader("Authorization");
        boolean isPrivate = planDTO.getIsShared() == 0;
        boolean existAuth = authHeader != null && authHeader.startsWith("Bearer ");

        if (existAuth) {
            int userIdx = handler.getUserIdxFromToken(authHeader);
            if (userIdx == planDTO.getUserIdx()) {
                planDTO.setIsWriter(1);
            }
            if (isPrivate && planDTO.getIsWriter() == 0) return null;
        }
        if (!existAuth && isPrivate) return null;

        // 연관 데이터 조회 (v2 활동 목록 사용)
        List<ActivityV2DTO> activities = planMapperV2.getActivitiesByPlanIdx(planIdx);
        planDTO.setTargetCateDTO(planMapper.getTargetCategoryByTargetIdx(planDTO.getTargetIdx()));
        planDTO.setFireInfo(planMapper.detailFireByPlanIdx(planIdx));
        planDTO.setVerifyCount(planMapper.getVerifyCountByPlanIdx(planIdx));

        int customJobIdx = categoryService.getIdxOfCustomJob();
        if (planDTO.getJobIdx() == customJobIdx) {
            planDTO.setJobEtcCateDTO(planMapper.getJobEtcInfoByPlanIdx(planIdx));
        } else {
            planDTO.setJobCateDTO(planMapper.getJOBCategoryByJobIdx(planDTO.getJobIdx()));
        }

        return planDetailMapper.toDto(planDTO, activities);
    }

    // ========================= 루틴 CRUD =========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createPlan(PlanCreateRequestV2 dto, int userIdx) {
        try {
            dto.setUserIdx(userIdx);
            int isCompleted = 0;
            int isDeleted = 0;
            int customJobIdx = categoryService.getIdxOfCustomJob();

            if (planMapper.getCntOfPlanByUserIdxNIsCompleted(userIdx, isCompleted, isDeleted) > 19) {
                return 412;
            }
            if (isUserDeleted(userIdx)) return 410;

            planMapperV2.insertPlanV2(dto);
            int planIdx = dto.getPlanIdx(); // useGeneratedKeys로 채워짐

            if (dto.getJobIdx() == customJobIdx && dto.getJobEtcCateDTO() != null) {
                JobEtcCateDTO jobEtcCateDTO = dto.getJobEtcCateDTO();
                jobEtcCateDTO.setPlanIdx(planIdx);
                planMapper.insertJobEtc(jobEtcCateDTO);
            }

            if (dto.isForked() && dto.getForkIdx() != null) {
                planMapper.modifyForkCount(dto.getForkIdx(), isDeleted);
            }

            return 201;
        } catch (Exception e) {
            log.error("createPlan error: ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 500;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePlan(int planIdx, PlanUpdateRequestV2 dto, int userIdx) {
        if (planNotFound(planIdx)) return 404;
        if (notOwner(planIdx, userIdx)) return 403;
        if (isUserDeleted(userIdx)) return 410;

        if (dto.getIsShared() != null && dto.getIsShared() == 1
                && planMapper.existsHandledReport(planIdx)) {
            return 409;
        }

        try {
            dto.setPlanIdx(planIdx);
            dto.setUserIdx(userIdx);
            planMapperV2.updatePlanPartial(dto);

            if (dto.getJobIdx() != null) {
                processJobEtcUpdate(planIdx, dto.getJobIdx(), dto.getJobEtcCateDTO());
            }
            return 200;
        } catch (Exception e) {
            log.error("updatePlan error: ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 500;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePlan(int planIdx, int userIdx) {
        if (planNotFound(planIdx)) return 404;
        if (notOwner(planIdx, userIdx)) return 403;
        if (isUserDeleted(userIdx)) return 410;

        try {
            planMapperV2.softDeletePlan(planIdx, userIdx);

            Integer forkIdx = planMapper.getForkIdxByPlanIdx(planIdx);
            if (forkIdx != null) {
                planMapper.modifyForkCount(forkIdx, 0);
            }
            return 200;
        } catch (Exception e) {
            log.error("deletePlan error: ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 500;
        }
    }

    // ========================= 활동 CRUD =========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createActivities(int planIdx, ActivityCreateRequestV2 dto, int userIdx) {
        if (planNotFound(planIdx)) return 404;
        if (notOwner(planIdx, userIdx)) return 403;
        if (isUserDeleted(userIdx)) return 410;

        try {
            for (ActivityItemV2 item : dto.getActivities()) {
                item.setPlanIdx(planIdx);
                planMapperV2.insertActivityV2(item);
            }
            return 201;
        } catch (Exception e) {
            log.error("createActivities error: ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 500;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateActivity(int planIdx, int activityIdx, ActivityUpdateRequestV2 dto, int userIdx) {
        if (planNotFound(planIdx)) return 404;
        if (notOwner(planIdx, userIdx)) return 403;
        if (activityNotFound(planIdx, activityIdx)) return 404;
        if (isUserDeleted(userIdx)) return 410;

        try {
            dto.setPlanIdx(planIdx);
            dto.setActivityIdx(activityIdx);
            planMapperV2.updateActivityPartial(dto);
            return 200;
        } catch (Exception e) {
            log.error("updateActivity error: ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 500;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteActivity(int planIdx, int activityIdx, int userIdx) {
        if (planNotFound(planIdx)) return 404;
        if (notOwner(planIdx, userIdx)) return 403;
        if (activityNotFound(planIdx, activityIdx)) return 404;
        if (isUserDeleted(userIdx)) return 410;

        try {
            planMapperV2.softDeleteActivity(planIdx, activityIdx);
            return 200;
        } catch (Exception e) {
            log.error("deleteActivity error: ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 500;
        }
    }

    // ========================= 루틴 추가 정보 조회 =========================

    @Override
    public PlanExtraInfoDTO getPlanExtraInfo(int planIdx, int userIdx) {
        if (planNotFound(planIdx)) return null;
        if (notOwner(planIdx, userIdx)) return null;
        return planMapperV2.getPlanExtraInfo(planIdx, userIdx);
    }

    // ========================= IMP 일괄 수정 =========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePlansImpBulk(BulkPlanImpUpdateRequest dto, int userIdx) {
        if (isUserDeleted(userIdx)) return 410;

        try {
            int affected = planMapperV2.updatePlansImpBulk(userIdx, dto.getPlanImps());
            if (affected != dto.getPlanImps().size()) return 403;
            return 200;
        } catch (Exception e) {
            log.error("updatePlansImpBulk error: ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 500;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateActivitiesImpBulk(int planIdx, BulkActivityImpUpdateRequest dto, int userIdx) {
        if (planNotFound(planIdx)) return 404;
        if (notOwner(planIdx, userIdx)) return 403;
        if (isUserDeleted(userIdx)) return 410;

        try {
            int affected = planMapperV2.updateActivitiesImpBulk(planIdx, dto.getActivityImps());
            if (affected != dto.getActivityImps().size()) return 404;
            return 200;
        } catch (Exception e) {
            log.error("updateActivitiesImpBulk error: ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return 500;
        }
    }

    // ========================= 내부 헬퍼 =========================

    private void processJobEtcUpdate(int planIdx, int jobIdx, JobEtcCateDTO jobEtcCateDTO) {
        int customJobIdx = categoryService.getIdxOfCustomJob();
        if (jobIdx != customJobIdx) return;
        if (jobEtcCateDTO == null) return;

        jobEtcCateDTO.setPlanIdx(planIdx);
        if (planMapper.checkJobEtcByPlanIdx(planIdx)) {
            planMapper.modifyJobEtc(jobEtcCateDTO);
        } else {
            planMapper.insertJobEtc(jobEtcCateDTO);
        }
    }

}
