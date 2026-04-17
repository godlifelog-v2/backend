package com.godLife.project.controller.v2;

import com.godLife.project.dto.request.plan.v2.ActivityCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.ActivityUpdateRequestV2;
import com.godLife.project.dto.request.plan.v2.BulkActivityDeleteRequest;
import com.godLife.project.dto.request.plan.v2.BulkActivityImpUpdateRequest;
import com.godLife.project.dto.request.plan.v2.BulkPlanImpUpdateRequest;
import com.godLife.project.dto.request.plan.v2.PlanCreateRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanForkRequestV2;
import com.godLife.project.dto.request.plan.v2.PlanUpdateRequestV2;
import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import com.godLife.project.dto.response.plan.v2.PlanExtraInfoDTO;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.service.interfaces.PlanService;
import com.godLife.project.service.interfaces.v2.PlanServiceV2;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v2/plan")
public class PlanControllerV2 {

    private final GlobalExceptionHandler handler;
    private final PlanServiceV2 planServiceV2;
    private final PlanService planService; // 조회수 증가 공용 사용

    // 쿠키 생성 메서드
    private Cookie createCookie(String key, String value, int maxAge, HttpServletRequest request) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        if (isSecure) {
            cookie.setSecure(true);
            cookie.setAttribute("SameSite", "None");
        }
        return cookie;
    }

    // ========================= 루틴 상세 조회 =========================

    @GetMapping("/detail/{planIdx}")
    public ResponseEntity<Map<String, Object>> detail(
            @PathVariable int planIdx,
            @CookieValue(value = "viewed_plans", required = false) String viewedPlans,
            HttpServletResponse response,
            HttpServletRequest request) {
        try {
            Map<Integer, Long> viewedPlansMap = new HashMap<>();
            if (viewedPlans != null && !viewedPlans.isEmpty()) {
                String[] arr = viewedPlans.split("_");
                for (int i = 0; i + 1 < arr.length; i += 2) {
                    try {
                        viewedPlansMap.put(Integer.parseInt(arr[i]), Long.parseLong(arr[i + 1]));
                    } catch (NumberFormatException e) {
                        log.warn("쿠키 파싱 실패 - index {}: {}_{}", i, arr[i], arr[i + 1]);
                    }
                }
            }

            long currentTime = System.currentTimeMillis();
            boolean isFirstView = !viewedPlansMap.containsKey(planIdx)
                    || currentTime - viewedPlansMap.get(planIdx) > 60 * 60 * 1000L;

            if (isFirstView) {
                planService.increaseView(planIdx);
                viewedPlansMap.put(planIdx, currentTime);
                StringBuilder updated = new StringBuilder();
                viewedPlansMap.forEach((k, v) -> updated.append(k).append("_").append(v).append("_"));
                response.addCookie(createCookie("viewed_plans", updated.toString(), 60 * 60, request));
            }

            PlanDetailDTO planDetailDTO = planServiceV2.detailRoutine(planIdx, 0, request);
            if (planDetailDTO == null) throw new NoSuchElementException("조회하려는 루틴이 존재하지 않습니다.");

            return ResponseEntity.ok().body(handler.createResponseWithData(200, "루틴 조회 성공", planDetailDTO));

        } catch (NoSuchElementException e) {
            log.info("PlanControllerV2 - detail :: {}", e.getMessage());
            return ResponseEntity.status(handler.getHttpStatus(404))
                    .body(handler.createResponse(404, "루틴 조회 실패 - 루틴이 존재하지 않습니다."));
        } catch (Exception e) {
            log.error("PlanControllerV2 - detail error", e);
            return ResponseEntity.status(handler.getHttpStatus(500))
                    .body(handler.createResponse(500, "서버 내부 오류로 루틴 조회에 실패했습니다."));
        }
    }

    // ========================= 루틴 추가 정보 조회 (인증) =========================

    @GetMapping("/auth/{planIdx}/extra")
    public ResponseEntity<Map<String, Object>> getPlanExtraInfo(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int planIdx) {
        try {
            int userIdx = handler.getUserIdxFromToken(authHeader);
            PlanExtraInfoDTO dto = planServiceV2.getPlanExtraInfo(planIdx, userIdx);
            if (dto == null) throw new NoSuchElementException("루틴을 찾을 수 없거나 접근 권한이 없습니다.");
            return ResponseEntity.ok().body(handler.createResponseWithData(200, "루틴 추가 정보 조회 성공", dto));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(handler.getHttpStatus(404))
                    .body(handler.createResponse(404, e.getMessage()));
        } catch (Exception e) {
            log.error("PlanControllerV2 - getPlanExtraInfo error", e);
            return ResponseEntity.status(handler.getHttpStatus(500))
                    .body(handler.createResponse(500, "서버 내부 오류로 루틴 추가 정보 조회에 실패했습니다."));
        }
    }

    // ========================= 루틴 생성 =========================

    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> createPlan(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody PlanCreateRequestV2 dto,
            BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(handler.getValidationErrors(result));
        }
        int userIdx = handler.getUserIdxFromToken(authHeader);
        int status = planServiceV2.createPlan(dto, userIdx);
        String msg = switch (status) {
            case 201 -> "루틴 생성 성공";
            case 410 -> "탈퇴한 유저는 루틴을 생성할 수 없습니다.";
            case 412 -> "루틴은 최대 5개까지 생성 가능합니다.";
            default  -> "서버 내부 오류로 루틴 생성에 실패했습니다.";
        };
        if (status == 201) {
            return ResponseEntity.status(handler.getHttpStatus(status))
                    .body(handler.createResponseWithData(status, msg, Map.of("planIdx", dto.getPlanIdx())));
        }
        return ResponseEntity.status(handler.getHttpStatus(status))
                .body(handler.createResponse(status, msg));
    }

    // ========================= 포크를 통한 루틴 생성 =========================

    @PostMapping("/auth/{sourcePlanIdx}/fork")
    public ResponseEntity<Map<String, Object>> forkPlan(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int sourcePlanIdx,
            @Valid @RequestBody PlanForkRequestV2 dto,
            BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(handler.getValidationErrors(result));
        }
        int userIdx = handler.getUserIdxFromToken(authHeader);
        int status = planServiceV2.forkPlan(sourcePlanIdx, dto, userIdx);
        String msg = switch (status) {
            case 201 -> "루틴 포크 성공";
            case 403 -> "비공개 루틴은 포크할 수 없습니다.";
            case 404 -> "원본 루틴이 존재하지 않습니다.";
            case 410 -> "탈퇴한 유저는 루틴을 생성할 수 없습니다.";
            case 412 -> "루틴은 최대 5개까지 생성 가능합니다.";
            default  -> "서버 내부 오류로 루틴 포크에 실패했습니다.";
        };
        if (status == 201) {
            return ResponseEntity.status(handler.getHttpStatus(status))
                    .body(handler.createResponseWithData(status, msg, Map.of("planIdx", dto.getPlanIdx())));
        }
        return ResponseEntity.status(handler.getHttpStatus(status))
                .body(handler.createResponse(status, msg));
    }

    // ========================= 루틴 부분 수정 =========================

    @PatchMapping("/auth/{planIdx}")
    public ResponseEntity<Map<String, Object>> updatePlan(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int planIdx,
            @RequestBody PlanUpdateRequestV2 dto) {
        int userIdx = handler.getUserIdxFromToken(authHeader);
        int status = planServiceV2.updatePlan(planIdx, dto, userIdx);
        String msg = switch (status) {
            case 200 -> "루틴 수정 성공";
            case 403 -> "루틴 수정 권한이 없습니다.";
            case 404 -> "수정하려는 루틴이 존재하지 않습니다.";
            case 409 -> "신고 처리된 루틴은 공개 설정할 수 없습니다.";
            case 410 -> "탈퇴한 유저는 루틴을 수정할 수 없습니다.";
            default  -> "서버 내부 오류로 루틴 수정에 실패했습니다.";
        };
        return ResponseEntity.status(handler.getHttpStatus(status))
                .body(handler.createResponse(status, msg));
    }

    // ========================= 루틴 삭제 =========================

    @DeleteMapping("/auth/{planIdx}")
    public ResponseEntity<Map<String, Object>> deletePlan(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int planIdx) {
        int userIdx = handler.getUserIdxFromToken(authHeader);
        int status = planServiceV2.deletePlan(planIdx, userIdx);
        String msg = switch (status) {
            case 200 -> "루틴 삭제 성공";
            case 403 -> "루틴 삭제 권한이 없습니다.";
            case 404 -> "삭제하려는 루틴이 존재하지 않습니다.";
            case 410 -> "탈퇴한 유저는 루틴을 삭제할 수 없습니다.";
            default  -> "서버 내부 오류로 루틴 삭제에 실패했습니다.";
        };
        return ResponseEntity.status(handler.getHttpStatus(status))
                .body(handler.createResponse(status, msg));
    }

    // ========================= 활동 생성 =========================

    @PostMapping("/auth/{planIdx}/activities")
    public ResponseEntity<Map<String, Object>> createActivities(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int planIdx,
            @Valid @RequestBody ActivityCreateRequestV2 dto,
            BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(handler.getValidationErrors(result));
        }
        int userIdx = handler.getUserIdxFromToken(authHeader);
        int status = planServiceV2.createActivities(planIdx, dto, userIdx);
        String msg = switch (status) {
            case 201 -> "활동 생성 성공";
            case 403 -> "활동 생성 권한이 없습니다.";
            case 404 -> "루틴이 존재하지 않습니다.";
            case 410 -> "탈퇴한 유저는 활동을 생성할 수 없습니다.";
            default  -> "서버 내부 오류로 활동 생성에 실패했습니다.";
        };
        return ResponseEntity.status(handler.getHttpStatus(status))
                .body(handler.createResponse(status, msg));
    }

    // ========================= 활동 부분 수정 =========================

    @PatchMapping("/auth/{planIdx}/activities/{activityIdx}")
    public ResponseEntity<Map<String, Object>> updateActivity(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int planIdx,
            @PathVariable int activityIdx,
            @RequestBody ActivityUpdateRequestV2 dto) {
        int userIdx = handler.getUserIdxFromToken(authHeader);
        int status = planServiceV2.updateActivity(planIdx, activityIdx, dto, userIdx);
        String msg = switch (status) {
            case 200 -> "활동 수정 성공";
            case 403 -> "활동 수정 권한이 없습니다.";
            case 404 -> "루틴 또는 활동이 존재하지 않습니다.";
            case 410 -> "탈퇴한 유저는 활동을 수정할 수 없습니다.";
            default  -> "서버 내부 오류로 활동 수정에 실패했습니다.";
        };
        return ResponseEntity.status(handler.getHttpStatus(status))
                .body(handler.createResponse(status, msg));
    }

    // ========================= 루틴 정렬 우선순위 일괄 수정 =========================

    @PatchMapping("/auth/bulk-imp")
    public ResponseEntity<Map<String, Object>> updatePlansImpBulk(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody BulkPlanImpUpdateRequest dto,
            BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(handler.getValidationErrors(result));
        }
        int userIdx = handler.getUserIdxFromToken(authHeader);
        int status = planServiceV2.updatePlansImpBulk(dto, userIdx);
        String msg = switch (status) {
            case 200 -> "루틴 정렬 우선순위 일괄 수정 성공";
            case 403 -> "소유하지 않은 루틴이 포함되어 있습니다.";
            case 410 -> "탈퇴한 유저는 루틴을 수정할 수 없습니다.";
            default  -> "서버 내부 오류로 루틴 정렬 우선순위 수정에 실패했습니다.";
        };
        return ResponseEntity.status(handler.getHttpStatus(status))
                .body(handler.createResponse(status, msg));
    }

    // ========================= 활동 정렬 우선순위 일괄 수정 =========================

    @PatchMapping("/auth/{planIdx}/activities/bulk-imp")
    public ResponseEntity<Map<String, Object>> updateActivitiesImpBulk(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int planIdx,
            @Valid @RequestBody BulkActivityImpUpdateRequest dto,
            BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(handler.getValidationErrors(result));
        }
        int userIdx = handler.getUserIdxFromToken(authHeader);
        int status = planServiceV2.updateActivitiesImpBulk(planIdx, dto, userIdx);
        String msg = switch (status) {
            case 200 -> "활동 정렬 우선순위 일괄 수정 성공";
            case 403 -> "활동 수정 권한이 없습니다.";
            case 404 -> "루틴 또는 활동이 존재하지 않습니다.";
            case 410 -> "탈퇴한 유저는 활동을 수정할 수 없습니다.";
            default  -> "서버 내부 오류로 활동 정렬 우선순위 수정에 실패했습니다.";
        };
        return ResponseEntity.status(handler.getHttpStatus(status))
                .body(handler.createResponse(status, msg));
    }

    // ========================= 활동 삭제 =========================

    @DeleteMapping("/auth/{planIdx}/activities/{activityIdx}")
    public ResponseEntity<Map<String, Object>> deleteActivity(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int planIdx,
            @PathVariable int activityIdx) {
        int userIdx = handler.getUserIdxFromToken(authHeader);
        int status = planServiceV2.deleteActivity(planIdx, activityIdx, userIdx);
        String msg = switch (status) {
            case 200 -> "활동 삭제 성공";
            case 403 -> "활동 삭제 권한이 없습니다.";
            case 404 -> "루틴 또는 활동이 존재하지 않습니다.";
            case 410 -> "탈퇴한 유저는 활동을 삭제할 수 없습니다.";
            default  -> "서버 내부 오류로 활동 삭제에 실패했습니다.";
        };
        return ResponseEntity.status(handler.getHttpStatus(status))
                .body(handler.createResponse(status, msg));
    }

    // ========================= 활동 일괄 삭제 =========================

    @DeleteMapping("/auth/{planIdx}/activities/bulk")
    public ResponseEntity<Map<String, Object>> deleteActivitiesBulk(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable int planIdx,
            @Valid @RequestBody BulkActivityDeleteRequest dto,
            BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(handler.getValidationErrors(result));
        }
        int userIdx = handler.getUserIdxFromToken(authHeader);
        int status = planServiceV2.deleteActivitiesBulk(planIdx, dto, userIdx);
        String msg = switch (status) {
            case 200 -> "활동 일괄 삭제 성공";
            case 403 -> "활동 삭제 권한이 없습니다.";
            case 404 -> "루틴 또는 활동이 존재하지 않습니다.";
            case 410 -> "탈퇴한 유저는 활동을 삭제할 수 없습니다.";
            default  -> "서버 내부 오류로 활동 일괄 삭제에 실패했습니다.";
        };
        return ResponseEntity.status(handler.getHttpStatus(status))
                .body(handler.createResponse(status, msg));
    }
}
