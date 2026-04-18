package com.godLife.project.enums;

import java.time.DayOfWeek;

public enum RepeatDay {
    SUN(1), MON(2), TUE(3), WED(4), THU(5), FRI(6), SAT(7);

    private final int dayIdx;

    RepeatDay(int dayIdx) {
        this.dayIdx = dayIdx;
    }

    public int getDayIdx() {
        return dayIdx;
    }

    /** "mon" → 2 */
    public static int toDayIdx(String day) {
        return valueOf(day.toUpperCase()).dayIdx;
    }

    /** java.time.DayOfWeek → DB DAY_IDX (1=일, 2=월 … 7=토) */
    public static int toDayIdx(DayOfWeek d) {
        return switch (d) {
            case SUNDAY    -> 1;
            case MONDAY    -> 2;
            case TUESDAY   -> 3;
            case WEDNESDAY -> 4;
            case THURSDAY  -> 5;
            case FRIDAY    -> 6;
            case SATURDAY  -> 7;
        };
    }
}
