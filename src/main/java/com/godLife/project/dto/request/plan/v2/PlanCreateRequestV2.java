package com.godLife.project.dto.request.plan.v2;

import com.godLife.project.dto.category.JobEtcCateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * v2 루틴 생성 전용 요청 DTO.
 * - 활동(Activity) 필드 없음 — 단일 책임 원칙 적용
 * - description, color 신규 필드 포함
 * - @NotNullJobEtc는 PlanDTO 전용이므로 미적용; jobEtcCateDTO 검증은 서비스에서 처리
 */
@Data
public class PlanCreateRequestV2 {

    @Schema(description = "루틴 인덱스 (생성 후 자동 채워짐)", hidden = true)
    private int planIdx;

    @Schema(description = "작성자 인덱스 (서버에서 토큰으로 설정)", hidden = true)
    private int userIdx;

    @Schema(description = "루틴 제목", example = "루틴 제목입니다.")
    @NotBlank(message = "{writePlan.planTitle.notBlank}")
    private String planTitle;

    @Schema(description = "목표 일 수 (99999 = 종료 없음)", example = "30")
    @Min(value = 7, message = "{writePlan.endTo.min}")
    private int endTo;

    @Schema(description = "반복 요일", example = "[\"mon\", \"wed\", \"fri\"]")
    private List<String> repeatDays;

    @Schema(description = "관심 카테고리 인덱스", example = "1")
    @Min(value = 1, message = "{writePlan.targetIdx.min}")
    private int targetIdx;

    @Schema(description = "직업 카테고리 인덱스", example = "1")
    @Min(value = 1, message = "{writePlan.jobIdx.min}")
    private int jobIdx;

    @Schema(description = "루틴 정렬 우선순위 (1=최하단, 10=최상단)", example = "1")
    @Min(value = 1, message = "{writePlan.planImp.min}")
    @Max(value = 10, message = "{writePlan.planImp.max}")
    private int planImp = 1;

    @Schema(description = "공개 여부 (0: 비공개, 1: 공개)", example = "0")
    private int isShared;

    @Schema(description = "즉시 활성화 여부 (0: 비활성, 1: 활성)", example = "0")
    private int isActive;

    @Schema(description = "루틴 간략 설명", example = "이 루틴은 체력 향상을 위한 루틴입니다.")
    private String description;

    @Schema(description = "루틴 고유 색상 (헥사코드, 알파값 포함)", example = "#FF5733FF")
    private String color;

    @Schema(description = "포크 여부", example = "false")
    private boolean forked;

    @Schema(description = "원본 루틴 인덱스 (포크 시)", example = "5")
    private Integer forkIdx;

    @Schema(description = "기타 직업 정보 (jobIdx가 기타 직업일 때 필수)")
    @Valid
    private JobEtcCateDTO jobEtcCateDTO;
}
