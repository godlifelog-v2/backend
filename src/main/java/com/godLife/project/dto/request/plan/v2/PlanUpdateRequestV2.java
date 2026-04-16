package com.godLife.project.dto.request.plan.v2;

import com.godLife.project.dto.category.JobEtcCateDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * v2 루틴 부분 수정 전용 요청 DTO.
 * - 모든 필드 nullable — null인 필드는 DB 업데이트에서 제외됨
 * - 유효성 어노테이션 없음 (부분 수정이므로 각 필드 선택적 전송)
 */
@Data
public class PlanUpdateRequestV2 {

    @Schema(description = "루틴 인덱스 (서버에서 path variable로 설정)", hidden = true)
    private int planIdx;

    @Schema(description = "작성자 인덱스 (서버에서 토큰으로 설정)", hidden = true)
    private int userIdx;

    @Schema(description = "루틴 제목", example = "수정된 루틴 제목")
    private String planTitle;

    @Schema(description = "목표 일 수 (99999 = 종료 없음)", example = "60")
    private Integer endTo;

    @Schema(description = "반복 요일", example = "[\"mon\", \"tue\"]")
    private List<String> repeatDays;

    @Schema(description = "관심 카테고리 인덱스", example = "2")
    private Integer targetIdx;

    @Schema(description = "직업 카테고리 인덱스", example = "3")
    private Integer jobIdx;

    @Schema(description = "공개 여부 (0: 비공개, 1: 공개)", example = "1")
    private Integer isShared;

    @Schema(description = "루틴 간략 설명", example = "수정된 루틴 설명입니다.")
    private String description;

    @Schema(description = "루틴 고유 색상 (헥사코드, 알파값 포함)", example = "#3498DBFF")
    private String color;

    @Schema(description = "기타 직업 정보 (jobIdx 변경 시)")
    private JobEtcCateDTO jobEtcCateDTO;
}
