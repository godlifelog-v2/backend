package com.godLife.project.service.impl.v2;

import com.godLife.project.dto.category.JobCateDTO;
import com.godLife.project.dto.category.TargetCateDTO;
import com.godLife.project.dto.model.common.FireDTO;
import com.godLife.project.dto.model.plan.ActivityDTO;
import com.godLife.project.dto.model.plan.PlanDTO;
import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.service.interfaces.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
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
    @Mock private CategoryService categoryService;
    @Mock private GlobalExceptionHandler handler;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private PlanServiceV2Impl planServiceV2;

    @Nested
    @DisplayName("detailRoutine - 비공개 루틴 접근 제한")
    class DetailRoutineAccessTest {

        private final int planIdx = 10;
        private final String tokenHeader = "Bearer test-token";

        @Test
        @DisplayName("비공개 루틴 - 다른 유저 토큰 → null 반환")
        void privateRoutine_anotherUser_returnsNull() {
            PlanDTO privatePlan = new PlanDTO();
            privatePlan.setIsShared(0); // 비공개
            privatePlan.setUserIdx(100);

            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(privatePlan);
            when(request.getHeader("Authorization")).thenReturn(tokenHeader);
            when(handler.getUserIdxFromToken(tokenHeader)).thenReturn(200); // 다른 유저

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

            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(privatePlan);
            when(request.getHeader("Authorization")).thenReturn(tokenHeader);
            when(handler.getUserIdxFromToken(tokenHeader)).thenReturn(100); // 본인
            when(planMapper.detailActivityByPlanIdx(planIdx)).thenReturn(List.of(new ActivityDTO()));
            when(planMapper.getTargetCategoryByTargetIdx(anyInt())).thenReturn(new TargetCateDTO());
            when(planMapper.detailFireByPlanIdx(planIdx)).thenReturn(new FireDTO());
            when(planMapper.getVerifyCountByPlanIdx(planIdx)).thenReturn(0);
            when(planMapper.getJOBCategoryByJobIdx(anyInt())).thenReturn(new JobCateDTO());
            when(categoryService.getIdxOfCustomJob()).thenReturn(19);

            PlanDetailDTO result = planServiceV2.detailRoutine(planIdx, 0, request);

            assertThat(result).isNotNull();
            assertThat(result.isWriter()).isTrue(); // boolean 단언
        }

        @Test
        @DisplayName("공개 루틴 - 다른 유저 토큰 → 조회 성공, isWriter = false")
        void publicRoutine_anotherUser_returnsDto_withIsWriterFalse() {
            PlanDTO publicPlan = new PlanDTO();
            publicPlan.setIsShared(1); // 공개
            publicPlan.setUserIdx(100);

            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(publicPlan);
            when(request.getHeader("Authorization")).thenReturn(tokenHeader);
            when(handler.getUserIdxFromToken(tokenHeader)).thenReturn(200); // 다른 유저
            when(planMapper.detailActivityByPlanIdx(planIdx)).thenReturn(List.of());
            when(planMapper.getTargetCategoryByTargetIdx(anyInt())).thenReturn(new TargetCateDTO());
            when(planMapper.detailFireByPlanIdx(planIdx)).thenReturn(new FireDTO());
            when(planMapper.getVerifyCountByPlanIdx(planIdx)).thenReturn(0);
            when(planMapper.getJOBCategoryByJobIdx(anyInt())).thenReturn(null);
            when(categoryService.getIdxOfCustomJob()).thenReturn(19);

            PlanDetailDTO result = planServiceV2.detailRoutine(planIdx, 0, request);

            assertThat(result).isNotNull();
            assertThat(result.isWriter()).isFalse(); // boolean 단언
        }
    }
}
