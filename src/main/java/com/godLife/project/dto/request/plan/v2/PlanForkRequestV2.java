package com.godLife.project.dto.request.plan.v2;

import com.godLife.project.dto.category.JobEtcCateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

/**
 * 포크를 통한 루틴 생성 요청 DTO.
 * null 필드는 원본 루틴 값을 그대로 사용하고, 값이 있는 필드만 오버라이드된다.
 * copyMode: 0=원본 활동 복사, 1=커스텀 활동(activities 필수), 2=활동 없이 루틴만 생성
 */
@Data
public class PlanForkRequestV2 {

    @Schema(description = "생성된 루틴 인덱스 (서버에서 채워짐)", hidden = true)
    private int planIdx;

    @Schema(description = "루틴 제목 (미입력 시 원본 값 사용)")
    private String planTitle;

    @Schema(description = "목표 일 수 (미입력 시 원본 값 사용)", example = "30")
    @Min(value = 7, message = "{writePlan.endTo.min}")
    private Integer endTo;

    @Schema(description = "반복 요일 (미입력 시 원본 값 사용)", example = "[\"mon\", \"wed\", \"fri\"]")
    private List<String> repeatDays;

    @Schema(description = "관심 카테고리 인덱스 (미입력 시 원본 값 사용)", example = "1")
    @Min(value = 1, message = "{writePlan.targetIdx.min}")
    private Integer targetIdx;

    @Schema(description = "직업 카테고리 인덱스 (미입력 시 원본 값 사용)", example = "1")
    @Min(value = 1, message = "{writePlan.jobIdx.min}")
    private Integer jobIdx;

    @Schema(description = "루틴 정렬 우선순위 (1=최하단, 10=최상단)", example = "1")
    @Min(value = 1, message = "{writePlan.planImp.min}")
    @Max(value = 10, message = "{writePlan.planImp.max}")
    private int planImp = 1;

    @Schema(description = "공개 여부 (0: 비공개, 1: 공개)", example = "0")
    private int isShared;

    @Schema(description = "즉시 활성화 여부 (0: 비활성, 1: 활성)", example = "0")
    private int isActive;

    @Schema(description = "루틴 간략 설명 (미입력 시 원본 값 사용)")
    private String description;

    @Schema(description = "루틴 고유 색상 (헥사코드, 미입력 시 원본 값 사용)", example = "#FF5733FF")
    private String color;

    @Schema(description = "기타 직업 정보 (jobIdx가 기타 직업일 때 필수)")
    @Valid
    private JobEtcCateDTO jobEtcCateDTO;

    @Schema(description = "활동 생성 모드 (0: 원본 활동 복사, 1: 커스텀 활동 지정, 2: 활동 없이 루틴만 생성)", example = "0")
    @Min(value = 0, message = "copyMode는 0, 1, 2 중 하나여야 합니다.")
    @Max(value = 2, message = "copyMode는 0, 1, 2 중 하나여야 합니다.")
    private int copyMode = 0;

    @Schema(description = "커스텀 활동 목록 (copyMode=1일 때 필수, 나머지는 null 가능)")
    @Valid
    private List<ActivityItemV2> activities;
}
