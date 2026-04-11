package com.godLife.project.service;

import com.godLife.project.dto.model.plan.ActivityDTO;
import com.godLife.project.dto.model.plan.PlanDTO;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.service.impl.PlanServiceImpl;
import com.godLife.project.service.interfaces.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanMapper planMapper;

    @Mock
    private CategoryService categoryService;

    @Mock
    private GlobalExceptionHandler handler;

    @InjectMocks
    private PlanServiceImpl planService;

    // ────────────────────────────────────────────────
    // Bug 3: getUserIsDeleted null 반환 시 NPE 수정
    // ────────────────────────────────────────────────
    @Nested
    @DisplayName("Bug 3: getUserIsDeleted NPE 수정")
    class GetUserIsDeletedNpeTest {

        @Test
        @DisplayName("insertPlanWithAct - getUserIsDeleted가 null 반환 시 NPE 없이 410 반환")
        void insertPlan_userNotFound_returns410WithoutNPE() {
            PlanDTO planDTO = new PlanDTO();
            planDTO.setUserIdx(999);
            planDTO.setActivities(List.of(new ActivityDTO()));

            when(planMapper.getCntOfPlanByUserIdxNIsCompleted(anyInt(), anyInt(), anyInt())).thenReturn(0);
            when(planMapper.getUserIsDeleted(999)).thenReturn(null); // DB에 없는 유저

            assertThatCode(() -> planService.insertPlanWithAct(planDTO))
                .doesNotThrowAnyException();

            assertThat(planService.insertPlanWithAct(planDTO)).isEqualTo(410);
        }

        @Test
        @DisplayName("insertPlanWithAct - getUserIsDeleted가 'Y' 반환 시 410 반환")
        void insertPlan_userDeleted_returns410() {
            PlanDTO planDTO = new PlanDTO();
            planDTO.setUserIdx(1);
            planDTO.setActivities(List.of(new ActivityDTO()));

            when(planMapper.getCntOfPlanByUserIdxNIsCompleted(anyInt(), anyInt(), anyInt())).thenReturn(0);
            when(planMapper.getUserIsDeleted(1)).thenReturn("Y");

            assertThat(planService.insertPlanWithAct(planDTO)).isEqualTo(410);
        }

        @Test
        @DisplayName("deletePlan - getUserIsDeleted가 null 반환 시 NPE 없이 410 반환")
        void deletePlan_userNotFound_returns410WithoutNPE() {
            int planIdx = 1, userIdx = 999;

            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(true);
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(userIdx);
            when(planMapper.getUserIsDeleted(userIdx)).thenReturn(null);

            assertThatCode(() -> planService.deletePlan(planIdx, userIdx))
                .doesNotThrowAnyException();

            assertThat(planService.deletePlan(planIdx, userIdx)).isEqualTo(410);
        }

        @Test
        @DisplayName("goStopPlan - getUserIsDeleted가 null 반환 시 NPE 없이 410 반환")
        void goStopPlan_userNotFound_returns410WithoutNPE() {
            int planIdx = 1, userIdx = 999;

            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(true);
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(userIdx);
            when(planMapper.getUserIsDeleted(userIdx)).thenReturn(null);

            assertThatCode(() -> planService.goStopPlan(planIdx, userIdx, 1, 0))
                .doesNotThrowAnyException();

            assertThat(planService.goStopPlan(planIdx, userIdx, 1, 0)).isEqualTo(410);
        }

        @Test
        @DisplayName("likePlan - getUserIsDeleted가 null 반환 시 NPE 없이 410 반환")
        void likePlan_userNotFound_returns410WithoutNPE() {
            int planIdx = 1, userIdx = 999;

            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(true);
            when(planMapper.checkLikeByPlanIdxNUserIdx(planIdx, userIdx)).thenReturn(false);
            when(planMapper.getUserIsDeleted(userIdx)).thenReturn(null);

            assertThatCode(() -> planService.likePlan(planIdx, userIdx, 0))
                .doesNotThrowAnyException();

            assertThat(planService.likePlan(planIdx, userIdx, 0)).isEqualTo(410);
        }

        @Test
        @DisplayName("updateEarlyComplete - getUserIsDeleted가 null 반환 시 NPE 없이 410 반환")
        void updateEarlyComplete_userNotFound_returns410WithoutNPE() {
            int planIdx = 1, userIdx = 999;

            when(planMapper.checkPlanByPlanIdx(planIdx, 0)).thenReturn(true);
            when(planMapper.getUserIdxByPlanIdx(planIdx)).thenReturn(userIdx);
            when(planMapper.checkActiveByPlanIdx(planIdx)).thenReturn(true);
            when(planMapper.checkCompleteByPlanIdx(planIdx, 1, 0)).thenReturn(false);
            when(planMapper.getUserIsDeleted(userIdx)).thenReturn(null);

            assertThatCode(() -> planService.updateEarlyComplete(planIdx, userIdx))
                .doesNotThrowAnyException();

            assertThat(planService.updateEarlyComplete(planIdx, userIdx)).isEqualTo(410);
        }
    }

    // ────────────────────────────────────────────────
    // Bug 4: 비공개 루틴 빈 DTO → null 반환 수정
    // ────────────────────────────────────────────────
    @Nested
    @DisplayName("Bug 4: 비공개 루틴 접근 반환값 수정")
    class PrivateRoutineAccessTest {

        @Test
        @DisplayName("비공개 루틴을 비작성자(토큰 있음)가 조회하면 null 반환 → 컨트롤러에서 404 응답")
        void privateRoutine_nonAuthorWithToken_returnsNull() {
            int planIdx = 1;
            PlanDTO privatePlan = new PlanDTO();
            privatePlan.setIsShared(0);   // 비공개
            privatePlan.setUserIdx(100);  // 작성자 userIdx

            HttpServletRequest request = mock(HttpServletRequest.class);
            String tokenHeader = "Bearer token.of.other.user";

            doNothing().when(planMapper).updateCompleteByPlanIdx(planIdx);
            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(privatePlan);
            when(request.getHeader("Authorization")).thenReturn(tokenHeader);
            when(handler.getUserIdxFromToken(tokenHeader)).thenReturn(200); // 다른 유저

            PlanDTO result = planService.detailRoutine(planIdx, 0, request);

            assertThat(result).isNull(); // 빈 DTO가 아닌 null 반환 확인
        }

        @Test
        @DisplayName("비공개 루틴을 토큰 없이 조회하면 null 반환 → 컨트롤러에서 404 응답")
        void privateRoutine_noToken_returnsNull() {
            int planIdx = 1;
            PlanDTO privatePlan = new PlanDTO();
            privatePlan.setIsShared(0);
            privatePlan.setUserIdx(100);

            HttpServletRequest request = mock(HttpServletRequest.class);

            doNothing().when(planMapper).updateCompleteByPlanIdx(planIdx);
            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(privatePlan);
            when(request.getHeader("Authorization")).thenReturn(null);

            PlanDTO result = planService.detailRoutine(planIdx, 0, request);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("비공개 루틴을 작성자 본인이 조회하면 정상 데이터 반환")
        void privateRoutine_asAuthor_returnsData() {
            int planIdx = 1, authorIdx = 100;
            PlanDTO privatePlan = new PlanDTO();
            privatePlan.setIsShared(0);
            privatePlan.setUserIdx(authorIdx);

            HttpServletRequest request = mock(HttpServletRequest.class);
            String tokenHeader = "Bearer author.token";

            doNothing().when(planMapper).updateCompleteByPlanIdx(planIdx);
            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(privatePlan);
            when(request.getHeader("Authorization")).thenReturn(tokenHeader);
            when(handler.getUserIdxFromToken(tokenHeader)).thenReturn(authorIdx);
            when(categoryService.getIdxOfCustomJob()).thenReturn(19);
            when(planMapper.detailActivityByPlanIdx(planIdx)).thenReturn(List.of());
            when(planMapper.getTargetCategoryByTargetIdx(anyInt())).thenReturn(null);
            when(planMapper.detailFireByPlanIdx(planIdx)).thenReturn(null);
            when(planMapper.getVerifyCountByPlanIdx(planIdx)).thenReturn(0);
            when(planMapper.getJOBCategoryByJobIdx(anyInt())).thenReturn(null);

            PlanDTO result = planService.detailRoutine(planIdx, 0, request);

            assertThat(result).isNotNull();
            assertThat(result.getIsWriter()).isEqualTo(1); // 작성자 플래그 확인
        }

        @Test
        @DisplayName("공개 루틴은 토큰 없이도 정상 데이터 반환")
        void publicRoutine_noToken_returnsData() {
            int planIdx = 2;
            PlanDTO publicPlan = new PlanDTO();
            publicPlan.setIsShared(1);  // 공개
            publicPlan.setUserIdx(100);

            HttpServletRequest request = mock(HttpServletRequest.class);

            doNothing().when(planMapper).updateCompleteByPlanIdx(planIdx);
            when(planMapper.detailPlanByPlanIdx(planIdx, 0)).thenReturn(publicPlan);
            when(request.getHeader("Authorization")).thenReturn(null);
            when(categoryService.getIdxOfCustomJob()).thenReturn(19);
            when(planMapper.detailActivityByPlanIdx(planIdx)).thenReturn(List.of());
            when(planMapper.getTargetCategoryByTargetIdx(anyInt())).thenReturn(null);
            when(planMapper.detailFireByPlanIdx(planIdx)).thenReturn(null);
            when(planMapper.getVerifyCountByPlanIdx(planIdx)).thenReturn(0);
            when(planMapper.getJOBCategoryByJobIdx(anyInt())).thenReturn(null);

            PlanDTO result = planService.detailRoutine(planIdx, 0, request);

            assertThat(result).isNotNull();
            assertThat(result.getIsWriter()).isEqualTo(0); // 작성자 아님
        }
    }
}
