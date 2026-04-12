package com.godLife.project.dto.request.plan.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * v2 활동 생성 전용 래퍼 DTO.
 * - planIdx는 path variable에서 처리
 * - 1개 이상의 활동 항목 필수
 */
@Data
public class ActivityCreateRequestV2 {

    @Schema(description = "생성할 활동 목록 (1개 이상)")
    @Size(min = 1, message = "{writePlan.activities.size}")
    @Valid
    private List<@NotNull(message = "{writePlan.activities.notNull}") ActivityItemV2> activities;
}
