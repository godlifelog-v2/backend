package com.godLife.project.dto.request.plan.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalTime;

/**
 * v2 단일 활동 생성 항목.
 * - description 없음 (DB 컬럼 삭제)
 * - event, duration 신규 필드 포함
 * - planIdx는 서비스에서 path variable로 설정
 */
@Data
public class ActivityItemV2 {

    @Schema(description = "활동 인덱스 (생성 후 서버에서 설정)", hidden = true)
    private int activityIdx;

    @Schema(description = "루틴 인덱스 (서버에서 설정)", hidden = true)
    private int planIdx;

    @Schema(description = "활동명", example = "조깅 30분")
    @NotBlank(message = "{writeActivity.activityName.notBlank}")
    private String activityName;

    @Schema(description = "알림 발송 시간 (HH:mm)", example = "07:30")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime setTime;

    @Schema(description = "활동 정렬 우선순위 (1=최하단, 20=최상단)", example = "1")
    @Min(value = 1, message = "{writeActivity.activityImp.min}")
    @Max(value = 20, message = "{writeActivity.activityImp.max}")
    private int activityImp = 1;

    @Schema(description = "알림 활성화 여부 (true: setTime 기준 SSE 알림 전송)", example = "false")
    private boolean event;

    @Schema(description = "활동 예상 소요 시간 (분 단위)", example = "30")
    private int duration;
}
