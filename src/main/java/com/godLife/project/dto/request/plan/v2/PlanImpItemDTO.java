package com.godLife.project.dto.request.plan.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "루틴 정렬 우선순위 수정 항목")
public class PlanImpItemDTO {

    @Schema(description = "루틴 인덱스", example = "123")
    private int planIdx;

    @Schema(description = "정렬 우선순위 (1~50)", example = "5")
    @Min(value = 1, message = "루틴의 정렬 우선순위는 1 이상이어야 합니다.")
    @Max(value = 50, message = "루틴의 정렬 우선순위는 50 이하이어야 합니다.")
    private int imp;
}
