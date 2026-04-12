package com.godLife.project.controller.v2;

import com.godLife.project.dto.response.plan.v2.PlanDetailDTO;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.service.interfaces.PlanService;
import com.godLife.project.service.interfaces.v2.PlanServiceV2;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

  // 쿠키 생성 메소드
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

  // 루틴 상세 보기 API — PlanDetailDTO 응답 (boolean 플래그, 읽기 전용 필드)
  @GetMapping("/detail/{planIdx}")
  public ResponseEntity<Map<String, Object>> detail(@PathVariable int planIdx,
                                                    @CookieValue(value = "viewed_plans", required = false) String viewedPlans,
                                                    HttpServletResponse response,
                                                    HttpServletRequest request) {
    try {
      Map<Integer, Long> viewedPlansMap = new HashMap<>();
      if (viewedPlans != null && !viewedPlans.isEmpty()) {
        String[] viewedPlansArray = viewedPlans.split("_");
        for (int i = 0; i + 1 < viewedPlansArray.length; i += 2) {
          try {
            int planId = Integer.parseInt(viewedPlansArray[i]);
            long timestamp = Long.parseLong(viewedPlansArray[i + 1]);
            viewedPlansMap.put(planId, timestamp);
          } catch (NumberFormatException e) {
            log.warn("쿠키 파싱 실패 - index {}: {}_{}", i, viewedPlansArray[i], viewedPlansArray[i + 1]);
          }
        }
      }

      // 조회수 증가 여부 체크
      long currentTime = System.currentTimeMillis();
      boolean isFirstView = !viewedPlansMap.containsKey(planIdx) || currentTime - viewedPlansMap.get(planIdx) > 60 * 60 * 1000;

      if (isFirstView) {
        log.debug("쿠키 없음 - 조회수 증가 처리 planIdx: {}", planIdx);
        planService.increaseView(planIdx);

        viewedPlansMap.put(planIdx, currentTime);

        StringBuilder updatedViewedPlans = new StringBuilder();
        for (Map.Entry<Integer, Long> entry : viewedPlansMap.entrySet()) {
          updatedViewedPlans.append(entry.getKey()).append("_").append(entry.getValue()).append("_");
        }
        response.addCookie(createCookie("viewed_plans", updatedViewedPlans.toString(), 60 * 60, request));
      } else {
        log.debug("쿠키 있음 - 조회수 유지 planIdx: {}", planIdx);
      }

      int isDeleted = 0;
      PlanDetailDTO planDetailDTO = planServiceV2.detailRoutine(planIdx, isDeleted, request);

      if (planDetailDTO == null) {
        throw new NoSuchElementException("조회하려는 루틴이 존재하지 않습니다.");
      }

      return ResponseEntity.ok().body(handler.createResponse(200, planDetailDTO));

    } catch (NoSuchElementException e) {
      String msg = "루틴 조회 실패,, 조회하려는 루틴이 존재하지 않습니다.";
      log.info("PlanControllerV2 - detail :: {}", e.getMessage());
      return ResponseEntity.status(handler.getHttpStatus(404)).body(handler.createResponse(404, msg));

    } catch (Exception e) {
      String msg = "서버 내부 오류로 인해 루틴 조회에 실패했습니다.";
      log.error("PlanControllerV2 - detail :: {}", msg, e);
      return ResponseEntity.status(handler.getHttpStatus(500)).body(handler.createResponse(500, msg));
    }
  }
}
