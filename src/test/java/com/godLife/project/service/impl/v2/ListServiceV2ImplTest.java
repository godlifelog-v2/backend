package com.godLife.project.service.impl.v2;

import com.godLife.project.dto.query.plan.v2.CustomPlanV2DTO;
import com.godLife.project.dto.response.plan.v2.MyPlanV2DTO;
import com.godLife.project.dto.response.plan.v2.TodayStatsDTO;
import com.godLife.project.mapper.ListMapper;
import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.mapper.VerifyMapper;
import com.godLife.project.mapper.v2.ListMapperV2;
import com.godLife.project.service.interfaces.CategoryService;
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
class ListServiceV2ImplTest {

    @Mock private ListMapperV2 listMapperV2;
    @Mock private ListMapper listMapper;
    @Mock private PlanMapper planMapper;
    @Mock private VerifyMapper verifyMapper;
    @Mock private CategoryService categoryService;

    @InjectMocks
    private ListServiceV2Impl listServiceV2Impl;

    @Nested
    @DisplayName("getMyPlansList")
    class GetMyPlansListTest {

        @Test
        @DisplayName("탈퇴한 유저 → null 반환")
        void deletedUser_returnsNull() {
            when(planMapper.getUserIsDeleted(1)).thenReturn("Y");

            List<MyPlanV2DTO> result = listServiceV2Impl.getMyPlansList(1);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("루틴 없음 → 빈 리스트 반환")
        void noPlans_returnsEmptyList() {
            when(planMapper.getUserIsDeleted(1)).thenReturn("N");
            when(listMapperV2.getMyPlansByUserIdx(1)).thenReturn(List.of());

            List<MyPlanV2DTO> result = listServiceV2Impl.getMyPlansList(1);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("루틴 있음 → MyPlanV2DTO 리스트 반환")
        void withPlans_returnsV2DtoList() {
            CustomPlanV2DTO dto = new CustomPlanV2DTO();
            dto.setPlanIdx(1);

            when(planMapper.getUserIsDeleted(1)).thenReturn("N");
            when(listMapperV2.getMyPlansByUserIdx(1)).thenReturn(List.of(dto));
            when(listMapper.getTargetIdxByPlanIdx(1)).thenReturn(1);
            when(listMapper.getJobIdxByPlanIdx(1)).thenReturn(1);
            when(planMapper.detailActivityByPlanIdx(1)).thenReturn(List.of());
            when(planMapper.getTargetCategoryByTargetIdx(1)).thenReturn(null);
            when(planMapper.detailFireByPlanIdx(1)).thenReturn(null);
            when(planMapper.getJOBCategoryByJobIdx(1)).thenReturn(null);
            when(categoryService.getIdxOfCustomJob()).thenReturn(19);

            List<MyPlanV2DTO> result = listServiceV2Impl.getMyPlansList(1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPlanInfos()).isEqualTo(dto);
        }
    }

    @Nested
    @DisplayName("getTodayStats")
    class GetTodayStatsTest {

        @Test
        @DisplayName("통계 조회 → TodayStatsDTO 반환")
        void getTodayStats_returnsStatsDto() {
            when(listMapperV2.getTodayPlanCount(eq(1), anyString())).thenReturn(5);
            when(listMapperV2.getTodayCompletedPlanCount(eq(1), anyString())).thenReturn(3);
            when(verifyMapper.getComboByUserIdx(1)).thenReturn(7);

            TodayStatsDTO stats = listServiceV2Impl.getTodayStats(1);

            assertThat(stats.getTotalToday()).isEqualTo(5);
            assertThat(stats.getCompletedToday()).isEqualTo(3);
            assertThat(stats.getCombo()).isEqualTo(7);
        }
    }
}
