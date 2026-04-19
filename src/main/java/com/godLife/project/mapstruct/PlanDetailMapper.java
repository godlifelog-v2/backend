package com.godLife.project.mapstruct;

import com.godLife.project.dto.model.plan.PlanDTO;
import com.godLife.project.dto.response.plan.v2.ActivityV2DTO;
import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * PlanDTO(int 플래그) → PlanDetailDTO(boolean 플래그) 변환 MapStruct 매퍼.
 *
 * <p>int→boolean 변환 대상 4개 필드(isShared/isActive/isCompleted/isWriter)는
 * MapStruct의 boolean 프로퍼티 네이밍 충돌을 피하기 위해 {@link AfterMapping}에서 처리한다.
 * 나머지 동일 타입/동일 이름 필드는 MapStruct가 자동으로 복사한다.
 */
@Mapper(componentModel = "spring")
public interface PlanDetailMapper {

    /**
     * @param src        v1 PlanDTO (int 플래그, DB 조회 결과)
     * @param activities v2 활동 목록 (별도 쿼리 결과)
     */
    @Mapping(target = "isShared",    ignore = true)
    @Mapping(target = "isActive",    ignore = true)
    @Mapping(target = "isCompleted", ignore = true)
    @Mapping(target = "isWriter",    ignore = true)
    @Mapping(target = "activities",  source = "activities")
    PlanDetailDTO toDto(PlanDTO src, List<ActivityV2DTO> activities);

    /** int(0/1) → boolean 변환. 수동 setter(setIsXxx)를 직접 호출해 JavaBean 네이밍 충돌 우회. */
    @AfterMapping
    default void convertIntFlags(PlanDTO src, @MappingTarget PlanDetailDTO dest) {
        dest.setIsShared    (src.getIsShared()    == 1);
        dest.setIsActive    (src.getIsActive()    == 1);
        dest.setIsCompleted (src.getIsCompleted() == 1);
        dest.setIsWriter    (src.getIsWriter()    == 1);
    }
}
