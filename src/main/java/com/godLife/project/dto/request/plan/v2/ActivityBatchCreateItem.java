package com.godLife.project.dto.request.plan.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalTime;

/**
 * 배치 처리용 활동 생성 항목.
 * clientTempId를 통해 order 배열에서 신규 항목의 위치를 지정할 수 있다.
 */
@Data
public class ActivityBatchCreateItem {

    @NotBlank(message = "clientTempId는 필수입니다.")
    @Schema(description = "클라이언트 임시 ID (order 배열에서 참조용)", example = "tmp-1")
    private String clientTempId;

    @NotBlank(message = "활동명은 필수입니다.")
    @Schema(description = "활동명", example = "새 활동")
    private String activityName;

    @Schema(description = "알림 발송 시간 (HH:mm)", example = "09:00")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime setTime;

    @Schema(description = "알림 활성화 여부", example = "false")
    private boolean event;

    @Schema(description = "활동 예상 소요 시간 (분 단위)", example = "30")
    private int duration;
}
