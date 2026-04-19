package com.godLife.project.mapper.v2;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlanRepeatDayMapper {

    void insertRepeatDays(@Param("planIdx") int planIdx,
                          @Param("dayIdxList") List<Integer> dayIdxList);

    void deleteByPlanIdx(@Param("planIdx") int planIdx);

    boolean existsByPlanIdxAndDayIdx(@Param("planIdx") int planIdx,
                                     @Param("dayIdx") int dayIdx);

    int countByPlanIdx(@Param("planIdx") int planIdx);

    /** DAY_IDX → "sun","mon"… 문자열 리스트 (응답 직렬화용) */
    List<String> getRepeatDayStringsByPlanIdx(@Param("planIdx") int planIdx);
}
