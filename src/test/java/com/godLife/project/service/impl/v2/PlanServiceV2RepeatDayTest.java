package com.godLife.project.service.impl.v2;

import com.godLife.project.dto.request.plan.v2.PlanCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanUpdateRequestV2;
import com.godLife.project.dto.request.verify.VerifyRequestDTO;
import com.godLife.project.enums.RepeatDay;
import com.godLife.project.mapper.PlanMapper;
import com.godLife.project.mapstruct.PlanDetailMapper;
import com.godLife.project.mapper.v2.PlanMapperV2;
import com.godLife.project.mapper.v2.PlanRepeatDayMapper;
import com.godLife.project.service.interfaces.CategoryService;
import com.godLife.project.service.interfaces.VerifyService;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanServiceV2RepeatDayTest {

    @Mock private PlanMapper planMapper;
    @Mock private PlanMapperV2 planMapperV2;
    @Mock private PlanRepeatDayMapper planRepeatDayMapper;
    @Mock private PlanDetailMapper planDetailMapper;
    @Mock private CategoryService categoryService;
    @Mock private VerifyService verifyService;

    @InjectMocks
    private PlanServiceV2Impl planServiceV2;

    private static final int PLAN_IDX = 1;
    private static final int USER_IDX = 10;
    private static final int ACTIVITY_IDX = 100;

    @BeforeEach
    void setUp() {
        when(categoryService.getIdxOfCustomJob()).thenReturn(19);
        when(planMapper.getUserIsDeleted(anyInt())).thenReturn("N");
        when(planMapper.checkPlanByPlanIdx(anyInt(), anyInt())).thenReturn(true);
        when(planMapper.getUserIdxByPlanIdx(anyInt())).thenReturn(USER_IDX);
        when(planMapper.checkActByActivityIdx(anyInt(), anyInt())).thenReturn(true);
        when(planMapper.checkActiveByPlanIdx(anyInt())).thenReturn(true);
    }

    // ========================= verifyActivityV2 요일 검증 =========================

    @Nested
    @DisplayName("verifyActivityV2 - 요일 검증")
    class VerifyDayValidationTest {

        @Test
        @DisplayName("오늘 요일이 루틴의 반복 요일에 없으면 status=400 반환")
        void dayMismatch_returns400() {
            int todayDayIdx = RepeatDay.toDayIdx(LocalDate.now().getDayOfWeek());
            when(planRepeatDayMapper.countByPlanIdx(PLAN_IDX)).thenReturn(3);
            when(planRepeatDayMapper.existsByPlanIdxAndDayIdx(PLAN_IDX, todayDayIdx)).thenReturn(false);

            Map<String, Object> result = planServiceV2.verifyActivityV2(PLAN_IDX, ACTIVITY_IDX, USER_IDX);

            assertThat(result.get("status")).isEqualTo(400);
            verify(verifyService, never()).verifyActivity(any());
        }

        @Test
        @DisplayName("오늘 요일이 루틴의 반복 요일에 있으면 verifyService에 위임")
        void dayMatch_delegatesToVerifyService() {
            int todayDayIdx = RepeatDay.toDayIdx(LocalDate.now().getDayOfWeek());
            when(planRepeatDayMapper.countByPlanIdx(PLAN_IDX)).thenReturn(3);
            when(planRepeatDayMapper.existsByPlanIdxAndDayIdx(PLAN_IDX, todayDayIdx)).thenReturn(true);
            when(verifyService.verifyActivity(any(VerifyRequestDTO.class))).thenReturn(200);
            when(planMapperV2.getActivitiesByPlanIdx(PLAN_IDX)).thenReturn(Collections.emptyList());

            Map<String, Object> result = planServiceV2.verifyActivityV2(PLAN_IDX, ACTIVITY_IDX, USER_IDX);

            assertThat(result.get("status")).isEqualTo(200);
            verify(verifyService, times(1)).verifyActivity(any(VerifyRequestDTO.class));
        }

        @Test
        @DisplayName("repeatDays가 없는 루틴(무제한)은 요일 검증 없이 verifyService에 위임")
        void noRepeatDays_skipsDayCheck_delegatesToVerifyService() {
            when(planRepeatDayMapper.countByPlanIdx(PLAN_IDX)).thenReturn(0);
            when(verifyService.verifyActivity(any(VerifyRequestDTO.class))).thenReturn(200);
            when(planMapperV2.getActivitiesByPlanIdx(PLAN_IDX)).thenReturn(Collections.emptyList());

            Map<String, Object> result = planServiceV2.verifyActivityV2(PLAN_IDX, ACTIVITY_IDX, USER_IDX);

            assertThat(result.get("status")).isEqualTo(200);
            verify(planRepeatDayMapper, never()).existsByPlanIdxAndDayIdx(anyInt(), anyInt());
            verify(verifyService, times(1)).verifyActivity(any(VerifyRequestDTO.class));
        }
    }

    // ========================= createPlan PLAN_REPEAT_DAYS =========================

    @Nested
    @DisplayName("createPlan - PLAN_REPEAT_DAYS 저장")
    class CreatePlanRepeatDaysTest {

        @Test
        @DisplayName("repeatDays=[\"mon\",\"fri\"] 이면 dayIdxList=[2,6]으로 insertRepeatDays 호출")
        void withRepeatDays_insertsRepeatDays() {
            PlanCreateRequestV2 dto = new PlanCreateRequestV2();
            dto.setUserIdx(USER_IDX);
            dto.setPlanTitle("테스트 루틴");
            dto.setEndTo(30);
            dto.setTargetIdx(1);
            dto.setJobIdx(1);
            dto.setRepeatDays(Arrays.asList("mon", "fri"));

            when(planMapper.getCntOfPlanByUserIdxNIsCompleted(anyInt(), anyInt(), anyInt())).thenReturn(0);
            doAnswer(inv -> {
                PlanCreateRequestV2 arg = inv.getArgument(0);
                arg.setPlanIdx(PLAN_IDX);
                return null;
            }).when(planMapperV2).insertPlanV2(any(PlanCreateRequestV2.class));

            planServiceV2.createPlan(dto, USER_IDX);

            verify(planRepeatDayMapper, times(1))
                    .insertRepeatDays(eq(PLAN_IDX), argThat(list ->
                            list.size() == 2 && list.contains(2) && list.contains(6)));
        }

        @Test
        @DisplayName("repeatDays=null 이면 insertRepeatDays 미호출")
        void withoutRepeatDays_skipsInsert() {
            PlanCreateRequestV2 dto = new PlanCreateRequestV2();
            dto.setUserIdx(USER_IDX);
            dto.setPlanTitle("테스트 루틴");
            dto.setEndTo(30);
            dto.setTargetIdx(1);
            dto.setJobIdx(1);
            dto.setRepeatDays(null);

            when(planMapper.getCntOfPlanByUserIdxNIsCompleted(anyInt(), anyInt(), anyInt())).thenReturn(0);
            doAnswer(inv -> {
                PlanCreateRequestV2 arg = inv.getArgument(0);
                arg.setPlanIdx(PLAN_IDX);
                return null;
            }).when(planMapperV2).insertPlanV2(any(PlanCreateRequestV2.class));

            planServiceV2.createPlan(dto, USER_IDX);

            verify(planRepeatDayMapper, never()).insertRepeatDays(anyInt(), any());
        }
    }

    // ========================= updatePlan PLAN_REPEAT_DAYS =========================

    @Nested
    @DisplayName("updatePlan - PLAN_REPEAT_DAYS 교체")
    class UpdatePlanRepeatDaysTest {

        @Test
        @DisplayName("repeatDays=[\"wed\"] 이면 기존 삭제 후 dayIdxList=[4]으로 재삽입")
        void withRepeatDays_deletesAndReinserts() {
            PlanUpdateRequestV2 dto = new PlanUpdateRequestV2();
            dto.setRepeatDays(Collections.singletonList("wed"));

            planServiceV2.updatePlan(PLAN_IDX, dto, USER_IDX);

            verify(planRepeatDayMapper, times(1)).deleteByPlanIdx(PLAN_IDX);
            verify(planRepeatDayMapper, times(1))
                    .insertRepeatDays(eq(PLAN_IDX), argThat(list ->
                            list.size() == 1 && list.contains(4)));
        }

        @Test
        @DisplayName("repeatDays=[] 이면 삭제만 하고 insertRepeatDays 미호출")
        void withEmptyRepeatDays_onlyDeletes() {
            PlanUpdateRequestV2 dto = new PlanUpdateRequestV2();
            dto.setRepeatDays(Collections.emptyList());

            planServiceV2.updatePlan(PLAN_IDX, dto, USER_IDX);

            verify(planRepeatDayMapper, times(1)).deleteByPlanIdx(PLAN_IDX);
            verify(planRepeatDayMapper, never()).insertRepeatDays(anyInt(), any());
        }

        @Test
        @DisplayName("repeatDays=null 이면 deleteByPlanIdx 미호출")
        void withNullRepeatDays_skipsAll() {
            PlanUpdateRequestV2 dto = new PlanUpdateRequestV2();
            dto.setRepeatDays(null);

            planServiceV2.updatePlan(PLAN_IDX, dto, USER_IDX);

            verify(planRepeatDayMapper, never()).deleteByPlanIdx(anyInt());
            verify(planRepeatDayMapper, never()).insertRepeatDays(anyInt(), any());
        }
    }

    // ========================= RepeatDay Enum 변환 =========================

    @Nested
    @DisplayName("RepeatDay enum 변환")
    class RepeatDayEnumTest {

        @Test
        @DisplayName("문자열 → dayIdx 변환 검증")
        void stringToDayIdx() {
            assertThat(RepeatDay.toDayIdx("sun")).isEqualTo(1);
            assertThat(RepeatDay.toDayIdx("mon")).isEqualTo(2);
            assertThat(RepeatDay.toDayIdx("tue")).isEqualTo(3);
            assertThat(RepeatDay.toDayIdx("wed")).isEqualTo(4);
            assertThat(RepeatDay.toDayIdx("thu")).isEqualTo(5);
            assertThat(RepeatDay.toDayIdx("fri")).isEqualTo(6);
            assertThat(RepeatDay.toDayIdx("sat")).isEqualTo(7);
        }

        @Test
        @DisplayName("DayOfWeek → dayIdx 변환 검증")
        void dayOfWeekToDayIdx() {
            assertThat(RepeatDay.toDayIdx(DayOfWeek.SUNDAY)).isEqualTo(1);
            assertThat(RepeatDay.toDayIdx(DayOfWeek.MONDAY)).isEqualTo(2);
            assertThat(RepeatDay.toDayIdx(DayOfWeek.TUESDAY)).isEqualTo(3);
            assertThat(RepeatDay.toDayIdx(DayOfWeek.WEDNESDAY)).isEqualTo(4);
            assertThat(RepeatDay.toDayIdx(DayOfWeek.THURSDAY)).isEqualTo(5);
            assertThat(RepeatDay.toDayIdx(DayOfWeek.FRIDAY)).isEqualTo(6);
            assertThat(RepeatDay.toDayIdx(DayOfWeek.SATURDAY)).isEqualTo(7);
        }
    }
}
