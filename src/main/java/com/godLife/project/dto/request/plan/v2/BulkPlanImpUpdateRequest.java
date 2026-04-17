package com.godLife.project.dto.request.plan.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "루틴 정렬 우선순위 일괄 수정 요청 DTO")
public class BulkPlanImpUpdateRequest {

    @Schema(description = "수정할 루틴 목록 (비어 있을 수 없음)")
    @NotEmpty(message = "수정할 루틴 목록이 비어 있습니다.")
    @Valid
    private List<PlanImpItemDTO> planImps;
}
