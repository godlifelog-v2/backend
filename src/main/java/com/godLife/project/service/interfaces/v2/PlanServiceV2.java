package com.godLife.project.service.interfaces.v2;

import com.godLife.project.dto.request.plan.v2.ActivityBatchRequestV2;
import com.godLife.project.dto.request.plan.v2.ActivityCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.ActivityUpdateRequestV2;
import com.godLife.project.dto.request.plan.v2.BulkActivityDeleteRequest;
import com.godLife.project.dto.request.plan.v2.BulkActivityImpUpdateRequest;
import com.godLife.project.dto.request.plan.v2.BulkPlanImpUpdateRequest;
import com.godLife.project.dto.request.plan.v2.PlanCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanForkRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanUpdateRequestV2;
import com.godLife.project.dto.response.plan.v2.ActivityV2DTO;
import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import com.godLife.project.dto.response.plan.v2.PlanExtraInfoDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface PlanServiceV2 {

    // 루틴과 활동 상세 조회 — PlanDetailDTO (boolean 플래그, 읽기 전용 응답)
    PlanDetailDTO detailRoutine(int planIdx, int isDeleted, HttpServletRequest request);

    // 루틴 추가 정보 조회 (포크·날짜·카운트·완료·후기)
    PlanExtraInfoDTO getPlanExtraInfo(int planIdx, int userIdx);

    // 루틴 생성 (활동 없이 단독 생성)
    int createPlan(PlanCreateRequestV2 dto, int userIdx);

    // 루틴 부분 수정 (null 필드는 업데이트 제외)
    int updatePlan(int planIdx, PlanUpdateRequestV2 dto, int userIdx);

    // 루틴 소프트 삭제
    int deletePlan(int planIdx, int userIdx);

    // 포크를 통한 루틴 생성 (공개 루틴만 가능, null 필드는 원본 값 사용)
    int forkPlan(int sourcePlanIdx, PlanForkRequestV2 dto, int userIdx);

    // 활동 생성 (특정 루틴에 활동 추가)
    int createActivities(int planIdx, ActivityCreateRequestV2 dto, int userIdx);

    // 활동 부분 수정 (null 필드는 업데이트 제외)
    int updateActivity(int planIdx, int activityIdx, ActivityUpdateRequestV2 dto, int userIdx);

    // 활동 소프트 삭제
    int deleteActivity(int planIdx, int activityIdx, int userIdx);

    // 활동 일괄 소프트 삭제
    int deleteActivitiesBulk(int planIdx, BulkActivityDeleteRequest dto, int userIdx);

    // 루틴 정렬 우선순위 일괄 수정
    int updatePlansImpBulk(BulkPlanImpUpdateRequest dto, int userIdx);

    // 활동 정렬 우선순위 일괄 수정
    int updateActivitiesImpBulk(int planIdx, BulkActivityImpUpdateRequest dto, int userIdx);

    // 활동 일괄 처리 (삭제/수정/생성/순서 단일 트랜잭션)
    // 반환: status(int) + 성공 시 activities(List<ActivityV2DTO>)
    java.util.Map<String, Object> batchUpdateActivities(int planIdx, ActivityBatchRequestV2 dto, int userIdx);

    // 활동 인증 v2 (변경된 활동 리스트 반환)
    java.util.Map<String, Object> verifyActivityV2(int planIdx, int activityIdx, int userIdx);
}
