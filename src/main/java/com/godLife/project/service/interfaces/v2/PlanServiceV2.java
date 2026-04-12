package com.godLife.project.service.interfaces.v2;

import com.godLife.project.dto.request.plan.v2.ActivityCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.ActivityUpdateRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanUpdateRequestV2;
import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface PlanServiceV2 {

    // 루틴과 활동 상세 조회 — PlanDetailDTO (boolean 플래그, 읽기 전용 응답)
    PlanDetailDTO detailRoutine(int planIdx, int isDeleted, HttpServletRequest request);

    // 루틴 생성 (활동 없이 단독 생성)
    int createPlan(PlanCreateRequestV2 dto, int userIdx);

    // 루틴 부분 수정 (null 필드는 업데이트 제외)
    int updatePlan(int planIdx, PlanUpdateRequestV2 dto, int userIdx);

    // 루틴 소프트 삭제
    int deletePlan(int planIdx, int userIdx);

    // 활동 생성 (특정 루틴에 활동 추가)
    int createActivities(int planIdx, ActivityCreateRequestV2 dto, int userIdx);

    // 활동 부분 수정 (null 필드는 업데이트 제외)
    int updateActivity(int planIdx, int activityIdx, ActivityUpdateRequestV2 dto, int userIdx);

    // 활동 소프트 삭제
    int deleteActivity(int planIdx, int activityIdx, int userIdx);
}
