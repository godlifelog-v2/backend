package com.godLife.project.service.impl.v2;

import com.godLife.project.dto.category.JobEtcCateDTO;
import com.godLife.project.dto.model.plan.PlanDTO;
import com.godLife.project.dto.request.plan.v2.*;
import com.godLife.project.dto.request.verify.VerifyRequestDTO;
import com.godLife.project.dto.response.plan.v2.ActivityV2DTO;
import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import com.godLife.project.dto.response.plan.v2.PlanExtraInfoDTO;
import com.godLife.project.enums.RepeatDay;
import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.mapstruct.PlanDetailMapper;
import com.godLife.project.mapper.v2.PlanMapperV2;
import com.godLife.project.mapper.v2.PlanRepeatDayMapper;
import com.godLife.project.service.interfaces.CategoryService;
import com.godLife.project.service.interfaces.VerifyService;
import com.godLife.project.service.interfaces.v2.PlanServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceV2Impl implements PlanServiceV2 {

    private final PlanMapper planMapper;
    private final PlanMapperV2 planMapperV2;
    private final PlanRepeatDayMapper planRepeatDayMapper;
    private final PlanDetailMapper planDetailMapper;
    private final CategoryService categoryService;
    private final VerifyService verifyService;

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
    public PlanDetailDTO detailRoutine(int planIdx, int isDeleted, int userIdx) {
        planMapper.updateCompleteByPlanIdx(planIdx);

        PlanDTO planDTO = planMapper.detailPlanByPlanIdx(planIdx, isDeleted);
        if (planDTO == null) return null;

        boolean isPrivate = planDTO.getIsShared() == 0;
        boolean isAuthenticated = userIdx > 0;

        if (isAuthenticated) {
            if (userIdx == planDTO.getUserIdx()) {
                planDTO.setIsWriter(1);
            }
            if (isPrivate && planDTO.getIsWriter() == 0) return null;
        }
        if (!isAuthenticated && isPrivate) return null;

        // V2: PLAN_REPEAT_DAYS에서 요일 정보 로드
        planDTO.setRepeatDays(planRepeatDayMapper.getRepeatDayStringsByPlanIdx(planIdx));

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

            if (dto.getRepeatDays() != null && !dto.getRepeatDays().isEmpty()) {
                List<Integer> dayIdxList = dto.getRepeatDays().stream()
                        .map(RepeatDay::toDayIdx).collect(Collectors.toList());
                planRepeatDayMapper.insertRepeatDays(planIdx, dayIdxList);
            }

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

            if (dto.getRepeatDays() != null) {
                planRepeatDayMapper.deleteByPlanIdx(planIdx);
                if (!dto.getRepeatDays().isEmpty()) {
                    List<Integer> dayIdxList = dto.getRepeatDays().stream()
                            .map(RepeatDay::toDayIdx).collect(Collectors.toList());
                    planRepeatDayMapper.insertRepeatDays(planIdx, dayIdxList);
                }
            }

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> forkPlan(int sourcePlanIdx, PlanForkRequestV2 dto, int userIdx) {
        Map<String, Object> result = new HashMap<>();
        try {
            PlanDTO source = planMapper.detailPlanByPlanIdx(sourcePlanIdx, 0);
            if (source == null) { result.put("status", 404); return result; }
            if (source.getIsShared() == 0) { result.put("status", 403); return result; }

            if (isUserDeleted(userIdx)) { result.put("status", 410); return result; }

            int copyMode = dto.getCopyMode();
            if (copyMode == 1 && (dto.getActivities() == null || dto.getActivities().isEmpty())) {
                result.put("status", 422);
                return result;
            }

            int isCompleted = 0;
            int isDeleted = 0;
            if (planMapper.getCntOfPlanByUserIdxNIsCompleted(userIdx, isCompleted, isDeleted) > 19) {
                result.put("status", 412);
                return result;
            }

            int customJobIdx = categoryService.getIdxOfCustomJob();
            int resolvedJobIdx = dto.getJobIdx() != null ? dto.getJobIdx() : source.getJobIdx();

            PlanCreateRequestV2 createDto = new PlanCreateRequestV2();
            createDto.setUserIdx(userIdx);
            createDto.setPlanTitle(dto.getPlanTitle() != null ? dto.getPlanTitle() : source.getPlanTitle());
            createDto.setEndTo(dto.getEndTo() != null ? dto.getEndTo() : source.getEndTo());
            createDto.setRepeatDays(dto.getRepeatDays() != null ? dto.getRepeatDays() : source.getRepeatDays());
            createDto.setTargetIdx(dto.getTargetIdx() != null ? dto.getTargetIdx() : source.getTargetIdx());
            createDto.setJobIdx(resolvedJobIdx);
            createDto.setPlanImp(dto.getPlanImp());
            createDto.setIsShared(dto.getIsShared());
            createDto.setIsActive(dto.getIsActive());
            createDto.setDescription(dto.getDescription() != null ? dto.getDescription() : source.getDescription());
            createDto.setColor(dto.getColor() != null ? dto.getColor() : source.getColor());
            createDto.setForked(true);
            createDto.setForkIdx(sourcePlanIdx);

            planMapperV2.insertPlanV2(createDto);
            int newPlanIdx = createDto.getPlanIdx();
            dto.setPlanIdx(newPlanIdx);

            if (createDto.getRepeatDays() != null && !createDto.getRepeatDays().isEmpty()) {
                List<Integer> dayIdxList = createDto.getRepeatDays().stream()
                        .map(RepeatDay::toDayIdx).collect(Collectors.toList());
                planRepeatDayMapper.insertRepeatDays(newPlanIdx, dayIdxList);
            }

            if (resolvedJobIdx == customJobIdx && dto.getJobEtcCateDTO() != null) {
                JobEtcCateDTO jobEtcCateDTO = dto.getJobEtcCateDTO();
                jobEtcCateDTO.setPlanIdx(newPlanIdx);
                planMapper.insertJobEtc(jobEtcCateDTO);
            }

            if (copyMode == 0) {
                // 원본 루틴의 활동 그대로 복사
                List<ActivityV2DTO> sourceActivities = planMapperV2.getActivitiesByPlanIdx(sourcePlanIdx);
                for (ActivityV2DTO srcAct : sourceActivities) {
                    ActivityItemV2 item = new ActivityItemV2();
                    item.setPlanIdx(newPlanIdx);
                    item.setActivityName(srcAct.getActivityName());
                    item.setSetTime(srcAct.getSetTime());
                    item.setActivityImp(srcAct.getActivityImp());
                    item.setEvent(srcAct.isEvent());
                    item.setDuration(srcAct.getDuration());
                    planMapperV2.insertActivityV2(item);
                }
            } else if (copyMode == 1) {
                // 클라이언트가 전달한 커스텀 활동 목록으로 생성
                for (ActivityItemV2 item : dto.getActivities()) {
                    item.setPlanIdx(newPlanIdx);
                    planMapperV2.insertActivityV2(item);
                }
            }
            // copyMode == 2: 활동 없이 루틴만 생성

            planMapper.modifyForkCount(sourcePlanIdx, isDeleted);

            List<ActivityV2DTO> activities = planMapperV2.getActivitiesByPlanIdx(newPlanIdx);
            result.put("status", 201);
            result.put("planIdx", newPlanIdx);
            result.put("activities", activities);
            return result;
        } catch (Exception e) {
            log.error("forkPlan error: ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result.put("status", 500);
            return result;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteActivitiesBulk(int planIdx, BulkActivityDeleteRequest dto, int userIdx) {
        if (planNotFound(planIdx)) return 404;
        if (notOwner(planIdx, userIdx)) return 403;
        if (isUserDeleted(userIdx)) return 410;

        try {
            int affected = planMapperV2.softDeleteActivitiesBulk(planIdx, dto.getActivityIdxList());
            if (affected != dto.getActivityIdxList().size()) return 404;
            return 200;
        } catch (Exception e) {
            log.error("deleteActivitiesBulk error: ", e);
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

    // ========================= 활동 배치 처리 =========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchUpdateActivities(int planIdx, ActivityBatchRequestV2 dto, int userIdx) {
        Map<String, Object> result = new HashMap<>();

        if (planNotFound(planIdx)) { result.put("status", 404); return result; }
        if (notOwner(planIdx, userIdx)) { result.put("status", 403); return result; }
        if (isUserDeleted(userIdx)) { result.put("status", 410); return result; }

        try {
            // 1. 삭제
            if (!dto.getDeleted().isEmpty()) {
                planMapperV2.softDeleteActivitiesBulk(planIdx, dto.getDeleted());
            }

            // 2. 수정 (낙관적 락 버전 체크)
            for (ActivityBatchUpdateItem item : dto.getUpdated()) {
                item.setPlanIdx(planIdx);
                int affected = planMapperV2.updateActivityWithVersion(item);
                if (affected == 0) {
                    result.put("status", 409);
                    result.put("error", "CONFLICT");
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return result;
                }
            }

            // 3. 생성 (clientTempId → activityIdx 맵 구성)
            Map<String, Integer> tempIdMap = new HashMap<>();
            for (ActivityBatchCreateItem c : dto.getCreated()) {
                ActivityItemV2 item = new ActivityItemV2();
                item.setPlanIdx(planIdx);
                item.setActivityName(c.getActivityName());
                item.setSetTime(c.getSetTime());
                item.setEvent(c.isEvent());
                item.setDuration(c.getDuration());
                item.setActivityImp(1);
                planMapperV2.insertActivityV2(item);
                tempIdMap.put(c.getClientTempId(), item.getActivityIdx());
            }

            // 4. 순서 처리
            if (!dto.getOrder().isEmpty()) {
                List<ActivityImpItemDTO> impList = new ArrayList<>();
                List<ActivityBatchOrderItem> order = dto.getOrder();
                int size = order.size();
                for (int i = 0; i < size; i++) {
                    ActivityBatchOrderItem orderItem = order.get(i);
                    Integer activityIdx;
                    if (orderItem.getActivityIdx() != null) {
                        activityIdx = orderItem.getActivityIdx();
                    } else if (orderItem.getClientTempId() != null) {
                        activityIdx = tempIdMap.get(orderItem.getClientTempId());
                        if (activityIdx == null) {
                            result.put("status", 400);
                            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                            return result;
                        }
                    } else {
                        result.put("status", 400);
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        return result;
                    }
                    ActivityImpItemDTO impItem = new ActivityImpItemDTO();
                    impItem.setActivityIdx(activityIdx);
                    impItem.setImp(size - i);
                    impList.add(impItem);
                }
                planMapperV2.updateActivitiesImpBulk(planIdx, impList);
            }

            List<ActivityV2DTO> activities = planMapperV2.getActivitiesByPlanIdx(planIdx);
            result.put("status", 200);
            result.put("activities", activities);
            return result;
        } catch (Exception e) {
            log.error("batchUpdateActivities error: ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result.put("status", 500);
            return result;
        }
    }

    // ========================= 활동 인증 v2 =========================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> verifyActivityV2(int planIdx, int activityIdx, int userIdx) {
        Map<String, Object> result = new HashMap<>();
        try {
            int todayDayIdx = RepeatDay.toDayIdx(LocalDate.now().getDayOfWeek());
            int repeatCount = planRepeatDayMapper.countByPlanIdx(planIdx);
            if (repeatCount > 0 && !planRepeatDayMapper.existsByPlanIdxAndDayIdx(planIdx, todayDayIdx)) {
                result.put("status", 400); // 오늘 요일 불일치
                return result;
            }

            VerifyRequestDTO verifyDto = new VerifyRequestDTO();
            verifyDto.setPlanIdx(planIdx);
            verifyDto.setActivityIdx(activityIdx);
            verifyDto.setUserIdx(userIdx);

            int status = verifyService.verifyActivity(verifyDto);
            result.put("status", status);

            if (status == 200) {
                result.put("activities", planMapperV2.getActivitiesByPlanIdx(planIdx));
            }
            return result;
        } catch (Exception e) {
            log.error("verifyActivityV2 error: ", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result.put("status", 500);
            return result;
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
