package com.godLife.project.dto.request.plan.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "활동 정렬 우선순위 수정 항목")
public class ActivityImpItemDTO {

    @Schema(description = "활동 인덱스", example = "456")
    private int activityIdx;

    @Schema(description = "정렬 우선순위 (1~20)", example = "3")
    @Min(value = 1, message = "활동의 정렬 우선순위는 1 이상이어야 합니다.")
    @Max(value = 20, message = "활동의 정렬 우선순위는 20 이하이어야 합니다.")
    private int imp;
}
