package com.godLife.project.mapper.v2;

import com.godLife.project.dto.request.plan.v2.ActivityImpItemDTO;
import com.godLife.project.dto.request.plan.v2.ActivityItemV2;
import com.godLife.project.dto.request.plan.v2.ActivityUpdateRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanImpItemDTO;
import com.godLife.project.dto.request.plan.v2.PlanCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanUpdateRequestV2;
import com.godLife.project.dto.response.plan.v2.ActivityV2DTO;
import com.godLife.project.dto.response.plan.v2.PlanExtraInfoDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlanMapperV2 {

    // 루틴 생성 (description, color 포함)
    void insertPlanV2(PlanCreateRequestV2 dto);

    // 루틴 부분 수정 (null 필드는 제외)
    int updatePlanPartial(PlanUpdateRequestV2 dto);

    // 루틴 소프트 삭제
    void softDeletePlan(@Param("planIdx") int planIdx, @Param("userIdx") int userIdx);

    // 활동 생성 (event, duration 포함, description 없음)
    void insertActivityV2(ActivityItemV2 dto);

    // 활동 부분 수정 (null 필드는 제외)
    int updateActivityPartial(ActivityUpdateRequestV2 dto);

    // 활동 소프트 삭제
    void softDeleteActivity(@Param("planIdx") int planIdx, @Param("activityIdx") int activityIdx);

    // v2 활동 목록 조회 (event, duration 포함, description 없음)
    List<ActivityV2DTO> getActivitiesByPlanIdx(int planIdx);

    // 루틴 추가 정보 조회 (포크·날짜·카운트·완료·후기)
    PlanExtraInfoDTO getPlanExtraInfo(@Param("planIdx") int planIdx, @Param("userIdx") int userIdx);

    // 루틴 정렬 우선순위 일괄 수정
    int updatePlansImpBulk(@Param("userIdx") int userIdx,
                           @Param("planImps") List<PlanImpItemDTO> planImps);

    // 활동 정렬 우선순위 일괄 수정
    int updateActivitiesImpBulk(@Param("planIdx") int planIdx,
                                @Param("activityImps") List<ActivityImpItemDTO> activityImps);
}
