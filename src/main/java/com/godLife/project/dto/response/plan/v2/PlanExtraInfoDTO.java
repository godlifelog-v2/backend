package com.godLife.project.dto.response.plan.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Schema(description = "루틴 추가 정보 응답 DTO")
public class PlanExtraInfoDTO {

    @Schema(description = "루틴 인덱스")
    private int planIdx;

    @Schema(description = "포크 원본 루틴 인덱스 (포크된 루틴일 경우)")
    private Integer forkIdx;

    @Schema(description = "포크 원본 루틴 제목 (forkIdx가 있을 경우)")
    private String forkTitle;

    @Schema(description = "루틴 생성일", example = "2026-04-16 02:12:48")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planSubDate;

    @Schema(description = "루틴 수정일", example = "2026-04-16 02:12:48")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planSubMod;

    @Schema(description = "루틴 시작일 (활성화된 경우)", example = "2026-04-16 02:12:48")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planSubStart;

    @Schema(description = "조회수")
    private int viewCount;

    @Schema(description = "포크수")
    private int forkCount;

    @Schema(description = "좋아요 수")
    private int likeCount;

    // MyBatis TINYINT(1) → boolean 호환: Lombok setXxx() 대신 setIsXxx() 수동 제공
    @Schema(description = "완료 여부")
    @Setter(AccessLevel.NONE)
    private boolean isCompleted;
    public void setIsCompleted(boolean isCompleted) { this.isCompleted = isCompleted; }

    @Schema(description = "루틴 후기 (완료 후 작성)")
    private String review;
}
