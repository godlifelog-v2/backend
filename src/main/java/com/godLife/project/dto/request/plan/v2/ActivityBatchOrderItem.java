package com.godLife.project.dto.request.plan.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 배치 처리용 활동 정렬 순서 항목.
 * activityIdx (기존 활동) 또는 clientTempId (신규 생성 활동) 중 하나를 반드시 지정해야 한다.
 */
@Data
public class ActivityBatchOrderItem {

    @Schema(description = "기존 활동 인덱스 (기존 활동인 경우)", example = "339")
    private Integer activityIdx;

    @Schema(description = "신규 활동의 클라이언트 임시 ID (created 항목인 경우)", example = "tmp-1")
    private String clientTempId;
}
