package com.godLife.project.dto.query.plan.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomPlanV2DTO {
  private int planIdx;
  private String planTitle;
  private int endTo;
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime planSubEnd;

  // MyBatis TINYINT(1) → boolean 호환: Lombok setXxx() 대신 setIsXxx() 수동 제공
  @Setter(AccessLevel.NONE)
  private boolean isShared;
  public void setIsShared(boolean isShared) { this.isShared = isShared; }

  @Setter(AccessLevel.NONE)
  private boolean isActive;
  public void setIsActive(boolean isActive) { this.isActive = isActive; }

  private int planImp;
  private int certExp;
  private List<String> repeatDays;
  private boolean fireState;
  private String color;
}
