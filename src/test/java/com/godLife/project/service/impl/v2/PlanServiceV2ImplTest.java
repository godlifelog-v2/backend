package com.godLife.project.service.impl.v2;

import com.godLife.project.dto.category.JobCateDTO;
import com.godLife.project.dto.category.TargetCateDTO;
import com.godLife.project.dto.model.common.FireDTO;
import com.godLife.project.dto.model.plan.PlanDTO;
import com.godLife.project.dto.response.plan.v2.ActivityV2DTO;
import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.mapper.dto.PlanDetailMapper;
import com.godLife.project.mapper.v2.PlanMapperV2;
import com.godLife.project.service.interfaces.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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
class PlanServiceV2ImplTest {

    @Mock private PlanMapper planMapper;
    @Mock private PlanMapperV2 planMapperV2;
    @Mock private PlanDetailMapper planDetailMapper;
    @Mock private CategoryService categoryService;
    @Mock private GlobalExceptionHandler handler;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private PlanServiceV2Impl planServiceV2;

    @BeforeEach
    void setUp() {
        when(categoryService.getIdxOfCustomJob()).thenReturn(19);

        // 매퍼 mock: PlanDTO의 int 플래그와 description/color를 그대로 반영하는 DTO 반환
        when(planDetailMapper.toDto(any(PlanDTO.class), anyList())).thenAnswer(inv -> {
            PlanDTO src = inv.getArgument(0);
            List<ActivityV2DTO> acts = inv.getArgument(1);
            PlanDetailDTO d = new PlanDetailDTO();
            d.setIsWriter    (src.getIsWriter()    == 1);
            d.setIsShared    (src.getIsShared()    == 1);
            d.setIsActive    (src.getIsActive()    == 1);
            d.setIsCompleted (src.getIsCompleted() == 1);
            d.setDescription (src.getDescription());
            d.setColor       (src.getColor());
            d.setActivities  (acts);
            return d;
        });
    }

    @Nested
    @DisplayName("detailRoutine - 비공개 루틴 접근 제한")
    class DetailRoutineAccessTest {

        private final int planIdx = 10;
        private final String tokenHeader = "Bearer test-token";

        @Test
        @DisplayName("비공개 루틴 - 다른 유저 토큰 → null 반환")
        void privateRoutine_anotherUser_returnsNull() {
            PlanDTO privatePlan = new PlanDTO();
            privatePlan.setIsShared(0);
            privatePlan.setUserIdx(100);

            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(privatePlan);
            when(request.getHeader("Authorization")).thenReturn(tokenHeader);
            when(handler.getUserIdxFromToken(tokenHeader)).thenReturn(200);

            PlanDetailDTO result = planServiceV2.detailRoutine(planIdx, 0, request);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("비공개 루틴 - 토큰 없음 → null 반환")
        void privateRoutine_noToken_returnsNull() {
            PlanDTO privatePlan = new PlanDTO();
            privatePlan.setIsShared(0);
            privatePlan.setUserIdx(100);

            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(privatePlan);
            when(request.getHeader("Authorization")).thenReturn(null);

            PlanDetailDTO result = planServiceV2.detailRoutine(planIdx, 0, request);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("비공개 루틴 - 작성자 본인 → 조회 성공, isWriter = true")
        void privateRoutine_owner_returnsDto_withIsWriterTrue() {
            PlanDTO privatePlan = new PlanDTO();
            privatePlan.setIsShared(0);
            privatePlan.setUserIdx(100);
            privatePlan.setPlanTitle("테스트 루틴");
            privatePlan.setDescription("테스트 설명");
            privatePlan.setColor("#FF5733FF");

            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(privatePlan);
            when(request.getHeader("Authorization")).thenReturn(tokenHeader);
            when(handler.getUserIdxFromToken(tokenHeader)).thenReturn(100);
            when(planMapperV2.getActivitiesByPlanIdx(planIdx)).thenReturn(List.of(new ActivityV2DTO()));
            when(planMapper.getTargetCategoryByTargetIdx(anyInt())).thenReturn(new TargetCateDTO());
            when(planMapper.detailFireByPlanIdx(planIdx)).thenReturn(new FireDTO());
            when(planMapper.getVerifyCountByPlanIdx(planIdx)).thenReturn(0);
            when(planMapper.getJOBCategoryByJobIdx(anyInt())).thenReturn(new JobCateDTO());

            PlanDetailDTO result = planServiceV2.detailRoutine(planIdx, 0, request);

            assertThat(result).isNotNull();
            assertThat(result.isWriter()).isTrue();
            assertThat(result.getDescription()).isEqualTo("테스트 설명");
            assertThat(result.getColor()).isEqualTo("#FF5733FF");
        }

        @Test
        @DisplayName("공개 루틴 - 다른 유저 토큰 → 조회 성공, isWriter = false")
        void publicRoutine_anotherUser_returnsDto_withIsWriterFalse() {
            PlanDTO publicPlan = new PlanDTO();
            publicPlan.setIsShared(1);
            publicPlan.setUserIdx(100);

            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(publicPlan);
            when(request.getHeader("Authorization")).thenReturn(tokenHeader);
            when(handler.getUserIdxFromToken(tokenHeader)).thenReturn(200);
            when(planMapperV2.getActivitiesByPlanIdx(planIdx)).thenReturn(List.of());
            when(planMapper.getTargetCategoryByTargetIdx(anyInt())).thenReturn(new TargetCateDTO());
            when(planMapper.detailFireByPlanIdx(planIdx)).thenReturn(new FireDTO());
            when(planMapper.getVerifyCountByPlanIdx(planIdx)).thenReturn(0);
            when(planMapper.getJOBCategoryByJobIdx(anyInt())).thenReturn(null);

            PlanDetailDTO result = planServiceV2.detailRoutine(planIdx, 0, request);

            assertThat(result).isNotNull();
            assertThat(result.isWriter()).isFalse();
        }

        @Test
        @DisplayName("v2 활동 목록 - ActivityV2DTO 반환, description 없음")
        void detailRoutine_returnsActivityV2DTOList() {
            PlanDTO publicPlan = new PlanDTO();
            publicPlan.setIsShared(1);
            publicPlan.setUserIdx(100);

            ActivityV2DTO activityV2DTO = new ActivityV2DTO();
            activityV2DTO.setActivityIdx(1);
            activityV2DTO.setActivityName("조깅 30분");
            activityV2DTO.setDuration(30);

            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(publicPlan);
            when(request.getHeader("Authorization")).thenReturn(null);
            when(planMapperV2.getActivitiesByPlanIdx(planIdx)).thenReturn(List.of(activityV2DTO));
            when(planMapper.getTargetCategoryByTargetIdx(anyInt())).thenReturn(new TargetCateDTO());
            when(planMapper.detailFireByPlanIdx(planIdx)).thenReturn(new FireDTO());
            when(planMapper.getVerifyCountByPlanIdx(planIdx)).thenReturn(0);
            when(planMapper.getJOBCategoryByJobIdx(anyInt())).thenReturn(new JobCateDTO());

            PlanDetailDTO result = planServiceV2.detailRoutine(planIdx, 0, request);

            assertThat(result).isNotNull();
            assertThat(result.getActivities()).hasSize(1);
            assertThat(result.getActivities().get(0).getActivityName()).isEqualTo("조깅 30분");
            assertThat(result.getActivities().get(0).getDuration()).isEqualTo(30);
        }
    }
}
