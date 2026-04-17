package com.godLife.project.dto.request.plan.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "활동 일괄 삭제 요청 DTO")
public class BulkActivityDeleteRequest {

    @Schema(description = "삭제할 활동 인덱스 목록 (비어 있을 수 없음)")
    @NotEmpty(message = "삭제할 활동 목록이 비어 있습니다.")
    private List<Integer> activityIdxList;
}
