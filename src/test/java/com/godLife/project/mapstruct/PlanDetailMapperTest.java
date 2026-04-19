package com.godLife.project.mapstruct;

import com.godLife.project.dto.category.JobCateDTO;
import com.godLife.project.dto.category.TargetCateDTO;
import com.godLife.project.dto.model.common.FireDTO;
import com.godLife.project.dto.model.plan.PlanDTO;
import com.godLife.project.dto.response.plan.v2.ActivityV2DTO;
import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanDetailMapperTest {

    private PlanDetailMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(PlanDetailMapper.class);
    }

    // ========================= int → boolean 변환 =========================

    @Nested
    @DisplayName("int 플래그 → boolean 변환")
    class FlagConversionTest {

        @Test
        @DisplayName("int 1 → boolean true")
        void flags_one_mapToTrue() {
            PlanDTO src = planDtoWithFlags(1, 1, 1, 1);

            PlanDetailDTO result = mapper.toDto(src, List.of());

            assertThat(result.isShared()).isTrue();
            assertThat(result.isActive()).isTrue();
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.isWriter()).isTrue();
        }

        @Test
        @DisplayName("int 0 → boolean false")
        void flags_zero_mapToFalse() {
            PlanDTO src = planDtoWithFlags(0, 0, 0, 0);

            PlanDetailDTO result = mapper.toDto(src, List.of());

            assertThat(result.isShared()).isFalse();
            assertThat(result.isActive()).isFalse();
            assertThat(result.isCompleted()).isFalse();
            assertThat(result.isWriter()).isFalse();
        }

        @Test
        @DisplayName("혼합 플래그 — 각 필드 독립 변환")
        void flags_mixed_mappedIndependently() {
            PlanDTO src = planDtoWithFlags(1, 0, 1, 0);

            PlanDetailDTO result = mapper.toDto(src, List.of());

            assertThat(result.isShared()).isTrue();
            assertThat(result.isActive()).isFalse();
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.isWriter()).isFalse();
        }
    }

    // ========================= 단순 필드 복사 =========================

    @Nested
    @DisplayName("단순 필드 복사")
    class SimpleFieldCopyTest {

        @Test
        @DisplayName("기본 루틴 정보 필드 복사")
        void basicPlanFields_copiedCorrectly() {
            PlanDTO src = new PlanDTO();
            src.setPlanIdx(42);
            src.setPlanTitle("테스트 루틴");
            src.setEndTo(30);
            src.setPlanImp(5);
            src.setCertExp(195);
            src.setViewCount(100);
            src.setLikeCount(10);
            src.setForkCount(2);
            src.setDescription("루틴 설명");
            src.setColor("#FF5733FF");
            src.setReview("후기 작성");
            src.setForkIdx(7);
            src.setForkTitle("원본 루틴");

            PlanDetailDTO result = mapper.toDto(src, List.of());

            assertThat(result.getPlanIdx()).isEqualTo(42);
            assertThat(result.getPlanTitle()).isEqualTo("테스트 루틴");
            assertThat(result.getEndTo()).isEqualTo(30);
            assertThat(result.getPlanImp()).isEqualTo(5);
            assertThat(result.getCertExp()).isEqualTo(195);
            assertThat(result.getViewCount()).isEqualTo(100);
            assertThat(result.getLikeCount()).isEqualTo(10);
            assertThat(result.getForkCount()).isEqualTo(2);
            assertThat(result.getDescription()).isEqualTo("루틴 설명");
            assertThat(result.getColor()).isEqualTo("#FF5733FF");
            assertThat(result.getReview()).isEqualTo("후기 작성");
            assertThat(result.getForkIdx()).isEqualTo(7);
            assertThat(result.getForkTitle()).isEqualTo("원본 루틴");
        }

        @Test
        @DisplayName("반복 요일 리스트 복사")
        void repeatDays_copiedCorrectly() {
            PlanDTO src = new PlanDTO();
            src.setRepeatDays(List.of("mon", "wed", "fri"));

            PlanDetailDTO result = mapper.toDto(src, List.of());

            assertThat(result.getRepeatDays()).containsExactly("mon", "wed", "fri");
        }

        @Test
        @DisplayName("날짜 필드 복사")
        void dateTimes_copiedCorrectly() {
            LocalDateTime now = LocalDateTime.now();
            PlanDTO src = new PlanDTO();
            src.setPlanSubDate(now);
            src.setPlanSubStart(now.minusDays(1));
            src.setPlanSubEnd(now.plusDays(30));

            PlanDetailDTO result = mapper.toDto(src, List.of());

            assertThat(result.getPlanSubDate()).isEqualTo(now);
            assertThat(result.getPlanSubStart()).isEqualTo(now.minusDays(1));
            assertThat(result.getPlanSubEnd()).isEqualTo(now.plusDays(30));
        }

        @Test
        @DisplayName("카테고리 및 fireInfo 참조 복사")
        void categoryAndFireInfo_copiedByReference() {
            JobCateDTO job = new JobCateDTO();
            TargetCateDTO target = new TargetCateDTO();
            FireDTO fire = new FireDTO();

            PlanDTO src = new PlanDTO();
            src.setJobCateDTO(job);
            src.setTargetCateDTO(target);
            src.setFireInfo(fire);

            PlanDetailDTO result = mapper.toDto(src, List.of());

            assertThat(result.getJobCateDTO()).isSameAs(job);
            assertThat(result.getTargetCateDTO()).isSameAs(target);
            assertThat(result.getFireInfo()).isSameAs(fire);
        }
    }

    // ========================= activities 파라미터 매핑 =========================

    @Nested
    @DisplayName("activities 파라미터 매핑")
    class ActivitiesMappingTest {

        @Test
        @DisplayName("activities 파라미터가 결과 DTO에 그대로 반영됨")
        void activities_mappedFromSecondParam() {
            ActivityV2DTO act = new ActivityV2DTO();
            act.setActivityIdx(1);
            act.setActivityName("조깅 30분");
            act.setDuration(30);

            PlanDetailDTO result = mapper.toDto(new PlanDTO(), List.of(act));

            assertThat(result.getActivities()).hasSize(1);
            assertThat(result.getActivities().get(0).getActivityName()).isEqualTo("조깅 30분");
            assertThat(result.getActivities().get(0).getDuration()).isEqualTo(30);
        }

        @Test
        @DisplayName("빈 활동 목록 → 빈 리스트")
        void emptyActivities_resultInEmptyList() {
            PlanDetailDTO result = mapper.toDto(new PlanDTO(), List.of());

            assertThat(result.getActivities()).isEmpty();
        }
    }

    // ========================= 헬퍼 =========================

    private PlanDTO planDtoWithFlags(int isShared, int isActive, int isCompleted, int isWriter) {
        PlanDTO dto = new PlanDTO();
        dto.setIsShared(isShared);
        dto.setIsActive(isActive);
        dto.setIsCompleted(isCompleted);
        dto.setIsWriter(isWriter);
        return dto;
    }
}
