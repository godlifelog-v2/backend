package com.godLife.project.dto.response.plan.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.godLife.project.dto.category.JobCateDTO;
import com.godLife.project.dto.category.JobEtcCateDTO;
import com.godLife.project.dto.category.TargetCateDTO;
import com.godLife.project.dto.model.common.FireDTO;
import com.godLife.project.dto.model.plan.ActivityDTO;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 루틴 상세 조회 전용 응답 DTO.
 * - PlanDTO(읽기/쓰기 공용)를 대체하여 조회 응답만을 위한 필드 구성
 * - int 플래그 → boolean 통일
 * - 불필요 필드 제거: userIdx, targetIdx, jobIdx, lastExp, isDeleted, deleteActivityIdx, planSubMod
 */
@Data
public class PlanDetailDTO {

    // 루틴 기본 정보
    private int planIdx;
    private String planTitle;
    private int endTo;
    private List<String> repeatDays;
    private int planImp;
    private int certExp;
    private int verifyCount;
    private int viewCount;
    private int likeCount;
    private int forkCount;

    // 상태 플래그 — boolean (MyBatis 수동 setter 패턴)
    @Setter(AccessLevel.NONE)
    private boolean isShared;
    public void setIsShared(boolean isShared) { this.isShared = isShared; }

    @Setter(AccessLevel.NONE)
    private boolean isActive;
    public void setIsActive(boolean isActive) { this.isActive = isActive; }

    @Setter(AccessLevel.NONE)
    private boolean isCompleted;
    public void setIsCompleted(boolean isCompleted) { this.isCompleted = isCompleted; }

    @Setter(AccessLevel.NONE)
    private boolean isWriter;
    public void setIsWriter(boolean isWriter) { this.isWriter = isWriter; }

    // 일반 boolean (is 접두사 없음 → Lombok 기본 사용)
    private boolean fireState;
    private boolean forked;

    // 날짜
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planSubDate;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planSubStart;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planSubEnd;

    // 포크 정보
    private Integer forkIdx;
    private String forkTitle;

    // 완료 후기
    private String review;

    // 활동 목록 (myPlans와 동일 키)
    private List<ActivityDTO> activities;

    // 카테고리 정보 (myPlans와 동일 키)
    private JobCateDTO jobCateDTO;
    private JobEtcCateDTO jobEtcCateDTO;
    private TargetCateDTO targetCateDTO;
    private FireDTO fireInfo;
}
