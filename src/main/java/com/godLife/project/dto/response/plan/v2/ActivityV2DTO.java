package com.godLife.project.dto.response.plan.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.time.LocalTime;

/**
 * v2 활동 응답 전용 DTO.
 * - description 없음 (DB 컬럼 삭제)
 * - event, duration 신규 필드 포함
 * - event: MyBatis TINYINT(1) → boolean 호환을 위해 수동 setter 패턴 적용
 */
@Data
public class ActivityV2DTO {

    @Schema(description = "활동 인덱스", example = "1")
    private int activityIdx;

    @Schema(description = "루틴 인덱스", example = "1")
    private int planIdx;

    @Schema(description = "활동명", example = "조깅 30분")
    private String activityName;

    @Schema(description = "알림 발송 시간", example = "07:30")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime setTime;

    @Schema(description = "활동 정렬 우선순위", example = "1")
    private int activityImp;

    @Schema(description = "오늘 인증 여부", example = "false")
    private boolean verified;

    // MyBatis TINYINT(1) → boolean 호환: Lombok setXxx() 대신 setIsXxx() 수동 제공
    @Setter(AccessLevel.NONE)
    @Schema(description = "알림 활성화 여부", example = "false")
    private boolean event;
    public void setIsEvent(boolean event) { this.event = event; }

    @Schema(description = "활동 예상 소요 시간 (분 단위)", example = "30")
    private int duration;

    @Schema(description = "낙관적 락 버전 (배치 수정 시 version 필드로 그대로 전송)", example = "0")
    private int version;
}
