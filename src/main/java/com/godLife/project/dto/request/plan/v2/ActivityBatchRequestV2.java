package com.godLife.project.dto.request.plan.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 활동 일괄 처리 요청 DTO.
 * deleted/updated/created/order 중 일부만 보내도 동작하며,
 * 전체가 단일 트랜잭션으로 처리된다.
 */
@Data
public class ActivityBatchRequestV2 {

    @Schema(description = "삭제할 활동 인덱스 목록")
    private List<Integer> deleted = Collections.emptyList();

    @Valid
    @Schema(description = "수정할 활동 목록 (version 포함 필수)")
    private List<ActivityBatchUpdateItem> updated = Collections.emptyList();

    @Valid
    @Schema(description = "신규 생성할 활동 목록")
    private List<ActivityBatchCreateItem> created = Collections.emptyList();

    @Schema(description = "최종 정렬 순서 (앞 = 높은 우선순위). activityIdx 또는 clientTempId 중 하나 필수.")
    private List<ActivityBatchOrderItem> order = Collections.emptyList();
}
