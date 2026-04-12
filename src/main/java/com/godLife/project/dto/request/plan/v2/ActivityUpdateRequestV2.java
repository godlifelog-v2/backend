package com.godLife.project.dto.request.plan.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalTime;

/**
 * v2 활동 부분 수정 전용 요청 DTO.
 * - 모든 필드 nullable — null인 필드는 DB 업데이트에서 제외됨
 * - planIdx, activityIdx는 서버에서 path variable로 설정
 */
@Data
public class ActivityUpdateRequestV2 {

    @Schema(description = "루틴 인덱스 (서버에서 path variable로 설정)", hidden = true)
    private int planIdx;

    @Schema(description = "활동 인덱스 (서버에서 path variable로 설정)", hidden = true)
    private int activityIdx;

    @Schema(description = "활동명", example = "수정된 활동명")
    private String activityName;

    @Schema(description = "알림 발송 시간 (HH:mm, null 전송 시 알림 시간 삭제)", example = "08:00")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime setTime;

    @Schema(description = "활동 정렬 우선순위 (1~20)", example = "3")
    private Integer activityImp;

    @Schema(description = "알림 활성화 여부", example = "true")
    private Boolean event;

    @Schema(description = "활동 예상 소요 시간 (분 단위)", example = "45")
    private Integer duration;
}
