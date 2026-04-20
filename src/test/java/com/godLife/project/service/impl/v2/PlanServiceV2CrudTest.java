package com.godLife.project.service.impl.v2;

import com.godLife.project.dto.request.plan.v2.ActivityCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.ActivityImpItemDTO;
import com.godLife.project.dto.request.plan.v2.ActivityItemV2;
import com.godLife.project.dto.request.plan.v2.ActivityUpdateRequestV2;
import com.godLife.project.dto.request.plan.v2.BulkActivityImpUpdateRequest;
import com.godLife.project.dto.request.plan.v2.BulkPlanImpUpdateRequest;
import com.godLife.project.dto.request.plan.v2.PlanCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanImpItemDTO;
import com.godLife.project.dto.request.plan.v2.PlanUpdateRequestV2;
import com.godLife.project.dto.response.plan.v2.PlanExtraInfoDTO;
import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.mapstruct.PlanDetailMapper;
import com.godLife.project.mapper.v2.PlanMapperV2;
import com.godLife.project.service.interfaces.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PlanServiceV2 CRUD 테스트")
class PlanServiceV2CrudTest {

    @Mock private PlanMapper planMapper;
    @Mock private PlanMapperV2 planMapperV2;
    @Mock private PlanDetailMapper planDetailMapper;
    @Mock private CategoryService categoryService;

    @InjectMocks
    private PlanServiceV2Impl planServiceV2;

    private final int userIdx = 1;
    private final int planIdx = 10;
    private final int activityIdx = 5;

    @BeforeEach
    void setUp() {
        when(categoryService.getIdxOfCustomJob()).thenReturn(19);
        when(planMapper.getUserIsDeleted(userIdx)).thenReturn("N");
    }

    // ===========================================
    // 루틴 생성
    // ===========================================

    @Nested
    @DisplayName("createPlan")
    class CreatePlanTest {

        private PlanCreateRequestV2 buildDto() {
            PlanCreateRequestV2 dto = new PlanCreateRequestV2();
            dto.setPlanTitle("테스트 루틴");
            dto.setEndTo(30);
            dto.setTargetIdx(1);
            dto.setJobIdx(1);
            dto.setPlanImp(1);
            dto.setDescription("루틴 설명");
            dto.setColor("#FF5733FF");
            return dto;
        }

        @Test
        @DisplayName("정상 생성 → 201")
        void createPlan_success_returns201() {
            when(planMapper.getCntOfPlanByUserIdxNIsCompleted(userIdx, 0, 0)).thenReturn(0);
            doNothing().when(planMapperV2).insertPlanV2(any());

            int result = planServiceV2.createPlan(buildDto(), userIdx);

            assertThat(result).isEqualTo(201);
            verify(planMapperV2).insertPlanV2(any());
        }

        @Test
        @DisplayName("루틴 20개 초과 → 412")
        void createPlan_overLimit_returns412() {
            when(planMapper.getCntOfPlanByUserIdxNIsCompleted(userIdx, 0, 0)).thenReturn(20);

            int result = planServiceV2.createPlan(buildDto(), userIdx);

            assertThat(result).isEqualTo(412);
            verify(planMapperV2, never()).insertPlanV2(any());
        }

        @Test
        @DisplayName("탈퇴한 유저 → 410")
        void createPlan_deletedUser_returns410() {
            when(planMapper.getCntOfPlanByUserIdxNIsCompleted(userIdx, 0, 0)).thenReturn(0);
            when(planMapper.getUserIsDeleted(userIdx)).thenReturn("Y");

            int result = planServiceV2.createPlan(buildDto(), userIdx);

            assertThat(result).isEqualTo(410);
            verify(planMapperV2, never()).insertPlanV2(any());
        }

        @Test
        @DisplayName("포크 루틴 생성 → fork 카운트 업데이트 호출")
        void createPlan_forked_callsModifyForkCount() {
            when(planMapper.getCntOfPlanByUserIdxNIsCompleted(userIdx, 0, 0)).thenReturn(0);
            doNothing().when(planMapperV2).insertPlanV2(any());

            PlanCreateRequestV2 dto = buildDto();
            dto.setForked(true);
            dto.setForkIdx(99);

            planServiceV2.createPlan(dto, userIdx);

            verify(planMapper).modifyForkCount(99, 0);
        }
    }

    // ===========================================
    // 루틴 수정
    // ===========================================

    @Nested
    @DisplayName("updatePlan")
    class UpdatePlanTest {

        @BeforeEach
        void planSetup() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(true);
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(userIdx);
        }

        @Test
        @DisplayName("부분 수정 성공 → 200")
        void updatePlan_success_returns200() {
            PlanUpdateRequestV2 dto = new PlanUpdateRequestV2();
            dto.setPlanTitle("수정된 제목");
            when(planMapperV2.updatePlanPartial(any())).thenReturn(1);

            int result = planServiceV2.updatePlan(planIdx, dto, userIdx);

            assertThat(result).isEqualTo(200);
            verify(planMapperV2).updatePlanPartial(any());
        }

        @Test
        @DisplayName("루틴 없음 → 404")
        void updatePlan_notFound_returns404() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(false);

            int result = planServiceV2.updatePlan(planIdx, new PlanUpdateRequestV2(), userIdx);

            assertThat(result).isEqualTo(404);
        }

        @Test
        @DisplayName("작성자 아님 → 403")
        void updatePlan_notOwner_returns403() {
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(999);

            int result = planServiceV2.updatePlan(planIdx, new PlanUpdateRequestV2(), userIdx);

            assertThat(result).isEqualTo(403);
        }

        @Test
        @DisplayName("신고 처리된 루틴 공개 설정 → 409")
        void updatePlan_reportedPlanShared_returns409() {
            PlanUpdateRequestV2 dto = new PlanUpdateRequestV2();
            dto.setIsShared(1);
            when(planMapper.existsHandledReport(planIdx)).thenReturn(true);

            int result = planServiceV2.updatePlan(planIdx, dto, userIdx);

            assertThat(result).isEqualTo(409);
        }
    }

    // ===========================================
    // 루틴 삭제
    // ===========================================

    @Nested
    @DisplayName("deletePlan")
    class DeletePlanTest {

        @BeforeEach
        void planSetup() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(true);
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(userIdx);
        }

        @Test
        @DisplayName("정상 삭제 → 200")
        void deletePlan_success_returns200() {
            when(planMapper.getForkIdxByPlanIdx(planIdx)).thenReturn(null);

            int result = planServiceV2.deletePlan(planIdx, userIdx);

            assertThat(result).isEqualTo(200);
            verify(planMapperV2).softDeletePlan(planIdx, userIdx);
        }

        @Test
        @DisplayName("포크된 루틴 삭제 → 원본 fork 카운트 감소")
        void deletePlan_forked_updatesForkCount() {
            when(planMapper.getForkIdxByPlanIdx(planIdx)).thenReturn(50);

            planServiceV2.deletePlan(planIdx, userIdx);

            verify(planMapper).modifyForkCount(50, 0);
        }

        @Test
        @DisplayName("루틴 없음 → 404")
        void deletePlan_notFound_returns404() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(false);

            int result = planServiceV2.deletePlan(planIdx, userIdx);

            assertThat(result).isEqualTo(404);
        }

        @Test
        @DisplayName("작성자 아님 → 403")
        void deletePlan_notOwner_returns403() {
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(999);

            int result = planServiceV2.deletePlan(planIdx, userIdx);

            assertThat(result).isEqualTo(403);
        }
    }

    // ===========================================
    // 활동 생성
    // ===========================================

    @Nested
    @DisplayName("createActivities")
    class CreateActivitiesTest {

        @BeforeEach
        void planSetup() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(true);
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(userIdx);
        }

        @Test
        @DisplayName("활동 생성 성공 → 201")
        void createActivities_success_returns201() {
            ActivityItemV2 item = new ActivityItemV2();
            item.setActivityName("조깅 30분");
            item.setActivityImp(1);

            ActivityCreateRequestV2 dto = new ActivityCreateRequestV2();
            dto.setActivities(List.of(item));

            int result = planServiceV2.createActivities(planIdx, dto, userIdx);

            assertThat(result).isEqualTo(201);
            verify(planMapperV2).insertActivityV2(item);
            assertThat(item.getPlanIdx()).isEqualTo(planIdx);
        }

        @Test
        @DisplayName("루틴 없음 → 404")
        void createActivities_planNotFound_returns404() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(false);

            int result = planServiceV2.createActivities(planIdx, new ActivityCreateRequestV2(), userIdx);

            assertThat(result).isEqualTo(404);
        }
    }

    // ===========================================
    // 활동 수정
    // ===========================================

    @Nested
    @DisplayName("updateActivity")
    class UpdateActivityTest {

        @BeforeEach
        void setup() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(true);
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(userIdx);
            when(planMapper.checkActByActivityIdx(planIdx, activityIdx)).thenReturn(true);
        }

        @Test
        @DisplayName("활동 부분 수정 성공 → 200")
        void updateActivity_success_returns200() {
            ActivityUpdateRequestV2 dto = new ActivityUpdateRequestV2();
            dto.setActivityName("수정된 활동명");
            when(planMapperV2.updateActivityPartial(any())).thenReturn(1);

            int result = planServiceV2.updateActivity(planIdx, activityIdx, dto, userIdx);

            assertThat(result).isEqualTo(200);
            verify(planMapperV2).updateActivityPartial(any());
        }

        @Test
        @DisplayName("활동 없음 → 404")
        void updateActivity_notFound_returns404() {
            when(planMapper.checkActByActivityIdx(planIdx, activityIdx)).thenReturn(false);

            int result = planServiceV2.updateActivity(planIdx, activityIdx, new ActivityUpdateRequestV2(), userIdx);

            assertThat(result).isEqualTo(404);
        }

        @Test
        @DisplayName("작성자 아님 → 403")
        void updateActivity_notOwner_returns403() {
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(999);

            int result = planServiceV2.updateActivity(planIdx, activityIdx, new ActivityUpdateRequestV2(), userIdx);

            assertThat(result).isEqualTo(403);
        }
    }

    // ===========================================
    // 루틴 추가 정보 조회
    // ===========================================

    @Nested
    @DisplayName("getPlanExtraInfo")
    class GetPlanExtraInfoTest {

        @BeforeEach
        void planSetup() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(true);
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(userIdx);
        }

        @Test
        @DisplayName("조회 성공 → DTO 반환")
        void getPlanExtraInfo_success_returnsDto() {
            PlanExtraInfoDTO dto = new PlanExtraInfoDTO();
            dto.setPlanIdx(planIdx);
            when(planMapperV2.getPlanExtraInfo(planIdx, userIdx)).thenReturn(dto);

            PlanExtraInfoDTO result = planServiceV2.getPlanExtraInfo(planIdx, userIdx);

            assertThat(result).isNotNull();
            assertThat(result.getPlanIdx()).isEqualTo(planIdx);
        }

        @Test
        @DisplayName("루틴 없음 → null 반환")
        void getPlanExtraInfo_planNotFound_returnsNull() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(false);

            PlanExtraInfoDTO result = planServiceV2.getPlanExtraInfo(planIdx, userIdx);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("타인 소유 → null 반환")
        void getPlanExtraInfo_notOwner_returnsNull() {
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(999);

            PlanExtraInfoDTO result = planServiceV2.getPlanExtraInfo(planIdx, userIdx);

            assertThat(result).isNull();
        }
    }

    // ===========================================
    // 루틴 IMP 일괄 수정
    // ===========================================

    @Nested
    @DisplayName("updatePlansImpBulk")
    class UpdatePlansImpBulkTest {

        private BulkPlanImpUpdateRequest buildDto() {
            PlanImpItemDTO item = new PlanImpItemDTO();
            item.setPlanIdx(planIdx);
            item.setImp(3);
            BulkPlanImpUpdateRequest dto = new BulkPlanImpUpdateRequest();
            dto.setPlanImps(List.of(item));
            return dto;
        }

        @Test
        @DisplayName("일괄 수정 성공 → 200")
        void updatePlansImpBulk_success_returns200() {
            when(planMapperV2.updatePlansImpBulk(eq(userIdx), any())).thenReturn(1);

            int result = planServiceV2.updatePlansImpBulk(buildDto(), userIdx);

            assertThat(result).isEqualTo(200);
            verify(planMapperV2).updatePlansImpBulk(eq(userIdx), any());
        }

        @Test
        @DisplayName("미소유 루틴 포함 (affected != size) → 403")
        void updatePlansImpBulk_notOwned_returns403() {
            when(planMapperV2.updatePlansImpBulk(eq(userIdx), any())).thenReturn(0);

            int result = planServiceV2.updatePlansImpBulk(buildDto(), userIdx);

            assertThat(result).isEqualTo(403);
        }

        @Test
        @DisplayName("탈퇴한 유저 → 410")
        void updatePlansImpBulk_deletedUser_returns410() {
            when(planMapper.getUserIsDeleted(userIdx)).thenReturn("Y");

            int result = planServiceV2.updatePlansImpBulk(buildDto(), userIdx);

            assertThat(result).isEqualTo(410);
            verify(planMapperV2, never()).updatePlansImpBulk(anyInt(), any());
        }
    }

    // ===========================================
    // 활동 IMP 일괄 수정
    // ===========================================

    @Nested
    @DisplayName("updateActivitiesImpBulk")
    class UpdateActivitiesImpBulkTest {

        @BeforeEach
        void planSetup() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(true);
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(userIdx);
        }

        private BulkActivityImpUpdateRequest buildDto() {
            ActivityImpItemDTO item = new ActivityImpItemDTO();
            item.setActivityIdx(activityIdx);
            item.setImp(2);
            BulkActivityImpUpdateRequest dto = new BulkActivityImpUpdateRequest();
            dto.setActivityImps(List.of(item));
            return dto;
        }

        @Test
        @DisplayName("일괄 수정 성공 → 200")
        void updateActivitiesImpBulk_success_returns200() {
            when(planMapperV2.updateActivitiesImpBulk(eq(planIdx), any())).thenReturn(1);

            int result = planServiceV2.updateActivitiesImpBulk(planIdx, buildDto(), userIdx);

            assertThat(result).isEqualTo(200);
            verify(planMapperV2).updateActivitiesImpBulk(eq(planIdx), any());
        }

        @Test
        @DisplayName("루틴 없음 → 404")
        void updateActivitiesImpBulk_planNotFound_returns404() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(false);

            int result = planServiceV2.updateActivitiesImpBulk(planIdx, buildDto(), userIdx);

            assertThat(result).isEqualTo(404);
        }

        @Test
        @DisplayName("루틴 권한 없음 → 403")
        void updateActivitiesImpBulk_notOwner_returns403() {
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(999);

            int result = planServiceV2.updateActivitiesImpBulk(planIdx, buildDto(), userIdx);

            assertThat(result).isEqualTo(403);
        }

        @Test
        @DisplayName("탈퇴한 유저 → 410")
        void updateActivitiesImpBulk_deletedUser_returns410() {
            when(planMapper.getUserIsDeleted(userIdx)).thenReturn("Y");

            int result = planServiceV2.updateActivitiesImpBulk(planIdx, buildDto(), userIdx);

            assertThat(result).isEqualTo(410);
        }

        @Test
        @DisplayName("존재하지 않는 활동 포함 (affected != size) → 404")
        void updateActivitiesImpBulk_activityNotFound_returns404() {
            when(planMapperV2.updateActivitiesImpBulk(eq(planIdx), any())).thenReturn(0);

            int result = planServiceV2.updateActivitiesImpBulk(planIdx, buildDto(), userIdx);

            assertThat(result).isEqualTo(404);
        }
    }

    // ===========================================
    // 활동 삭제
    // ===========================================

    @Nested
    @DisplayName("deleteActivity")
    class DeleteActivityTest {

        @BeforeEach
        void setup() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(true);
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(userIdx);
            when(planMapper.checkActByActivityIdx(planIdx, activityIdx)).thenReturn(true);
        }

        @Test
        @DisplayName("활동 삭제 성공 → 200")
        void deleteActivity_success_returns200() {
            int result = planServiceV2.deleteActivity(planIdx, activityIdx, userIdx);

            assertThat(result).isEqualTo(200);
            verify(planMapperV2).softDeleteActivity(planIdx, activityIdx);
        }

        @Test
        @DisplayName("활동 없음 → 404")
        void deleteActivity_notFound_returns404() {
            when(planMapper.checkActByActivityIdx(planIdx, activityIdx)).thenReturn(false);

            int result = planServiceV2.deleteActivity(planIdx, activityIdx, userIdx);

            assertThat(result).isEqualTo(404);
        }

        @Test
        @DisplayName("루틴 없음 → 404")
        void deleteActivity_planNotFound_returns404() {
            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(false);

            int result = planServiceV2.deleteActivity(planIdx, activityIdx, userIdx);

            assertThat(result).isEqualTo(404);
        }
    }
}
