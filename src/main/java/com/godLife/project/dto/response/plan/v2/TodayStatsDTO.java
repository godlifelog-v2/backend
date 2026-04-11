package com.godLife.project.dto.response.plan.v2;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TodayStatsDTO {
    private int totalToday;
    private int completedToday;
    private int combo;
}
