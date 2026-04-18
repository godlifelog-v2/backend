package com.godLife.project.dto.request.plan.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

/**
 * 배치 처리용 활동 수정 항목.
 * version은 낙관적 락 검증에 사용된다.
 * null 필드는 DB 업데이트에서 제외된다.
 */
@Data
public class ActivityBatchUpdateItem {

    @Schema(description = "루틴 인덱스 (서버에서 설정)", hidden = true)
    private int planIdx;

    @NotNull(message = "activityIdx는 필수입니다.")
    @Schema(description = "수정할 활동 인덱스", example = "339")
    private int activityIdx;

    @NotNull(message = "version은 필수입니다.")
    @Schema(description = "낙관적 락 버전 (응답 받은 version 값 그대로 전송)", example = "0")
    private int version;

    @Schema(description = "활동명", example = "수정된 활동명")
    private String activityName;

    @Schema(description = "알림 발송 시간 (HH:mm)", example = "08:00")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime setTime;

    @Schema(description = "알림 활성화 여부", example = "true")
    private Boolean event;

    @Schema(description = "활동 예상 소요 시간 (분 단위)", example = "45")
    private Integer duration;
}
