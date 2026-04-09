package com.godLife.project.controller.v1;


import com.godLife.project.dto.model.plan.PlanDTO;
import com.godLife.project.dto.request.plan.PlanRequestDTO;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.service.interfaces.PlanService;
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
@RequestMapping("/api/v1/plan")
public class PlanController {

  private final GlobalExceptionHandler handler;

  private final PlanService planService;

  // 쿠키 생성 메소드
  private Cookie createCookie(String key, String value, int maxAge, HttpServletRequest request) {

    Cookie cookie = new Cookie(key, value);
    cookie.setMaxAge(maxAge);
    cookie.setPath("/");
    cookie.setHttpOnly(true);

    // 🔹 현재 요청이 HTTPS인지 확인하여 Secure 적용
    boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    if (isSecure) {
      cookie.setSecure(true);
      cookie.setAttribute("SameSite", "None");
    }

    return cookie;
  }

  // 루틴 작성 API
  @PostMapping("/auth/write")
  public ResponseEntity<Map<String, Object>> write(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody PlanDTO writePlanDTO, BindingResult result) {

    if (result.hasErrors()) {
      return ResponseEntity.badRequest().body(handler.getValidationErrors(result));
    }
    // userIdx 조회
    int userIdx = handler.getUserIdxFromToken(authHeader);
    writePlanDTO.setUserIdx(userIdx);
    int insertResult = planService.insertPlanWithAct(writePlanDTO);

    // 응답 메세지 세팅
    String msg = "";
    switch (insertResult) {
      case 201 -> msg = "루틴 저장 완료";
      case 410 -> msg = "회원 탈퇴한 계정입니다.";
      case 412 -> msg = "루틴 작성은 최대 5개만 가능합니다. 현재 작성한 루틴을 지우거나, 목표치 까지 완료해주세요.";
      case 500 -> msg = "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> msg = "알 수 없는 오류가 발생했습니다.";
    }

    // 응답 메시지 설정
    return ResponseEntity.status(handler.getHttpStatus(insertResult)).body(handler.createResponse(insertResult, msg));
  }


  // 루틴 상세 보기 API
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
        // 조회수 증가
        planService.increaseView(planIdx);

        // 현재 시간 기록
        viewedPlansMap.put(planIdx, currentTime);

        // 쿠키 값 업데이트
        StringBuilder updatedViewedPlans = new StringBuilder();
        for (Map.Entry<Integer, Long> entry : viewedPlansMap.entrySet()) {
          updatedViewedPlans.append(entry.getKey()).append("_").append(entry.getValue()).append("_");
        }

        // 마지막에 추가된 값으로 쿠키 설정
        response.addCookie(createCookie("viewed_plans", updatedViewedPlans.toString(), 60 * 60, request));
      } else {
        log.debug("쿠키 있음 - 조회수 유지 planIdx: {}", planIdx);
      }

      // 삭제 여부 설정   0: 삭제 X 1: 삭제 O
      int isDeleted = 0;
      // 해당 인덱스의 루틴 조회
      PlanDTO planDTO = planService.detailRoutine(planIdx, isDeleted, request);

      // planDTO가 null이면 예외 발생
      if (planDTO == null) {
        throw new NoSuchElementException("조회하려는 루틴이 존재하지 않습니다.");
      }

      // 응답 메시지 설정
      return ResponseEntity.ok().body(handler.createResponse(200, planDTO));

    } catch (NoSuchElementException e) {
      String msg = "루틴 조회 실패,, 조회하려는 루틴이 존재하지 않습니다.";
      log.info("PlanController - detail :: {}", e.getMessage());
      return ResponseEntity.status(handler.getHttpStatus(404)).body(handler.createResponse(404, msg));

    } catch (Exception e) {
      String msg = "서버 내부 오류로 인해 루틴 조회에 실패했습니다.";
      log.error("PlanController - detail :: {}", msg, e);
      return ResponseEntity.status(handler.getHttpStatus(500)).body(handler.createResponse(500, msg));
    }
  }


  // 루틴 수정 API
  @PatchMapping("/auth/modify")
  public ResponseEntity<Map<String, Object>> modify(@RequestHeader("Authorization") String authHeader, @Valid @RequestBody PlanDTO modifyPlanDTO, BindingResult result) {
    // 유효성 검사 실패 시 에러 반환
    if (result.hasErrors()) {
      return ResponseEntity.badRequest().body(handler.getValidationErrors(result));
    }

    // 삭제 여부 확인
    int isDeleted = 0;

    // userIdx 조회
    int userIdx = handler.getUserIdxFromToken(authHeader);
    modifyPlanDTO.setUserIdx(userIdx);

    // 서비스 로직 실행
    int modifyResult = planService.modifyPlanWithAct(modifyPlanDTO, isDeleted);
    // 응답 메세지 세팅
    String msg = "";
    switch (modifyResult) {
      case 200 -> msg = "루틴 수정 완료";
      case 403 -> msg = "작성자가 아닙니다. 재로그인 해주세요.";
      case 404 -> msg = "요청하신 루틴이 존재하지 않습니다.";
      case 409 -> msg = "신고 처리된 루틴은 공개로 설정할 수 없습니다.";
      case 410 -> msg = "회원 탈퇴한 계정입니다.";
      case 500 -> msg = "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> msg = "알 수 없는 오류가 발생했습니다.";
    }

    // 응답 메시지 설정
    return ResponseEntity.status(handler.getHttpStatus(modifyResult))
        .body(handler.createResponse(modifyResult, msg));
  }


  // 루틴 삭제 API
  @PatchMapping("/auth/delete/{planIdx}")
  public ResponseEntity<Map<String, Object>> delete(@RequestHeader("Authorization") String authHeader,
                                                    @PathVariable int planIdx) {
    // userIdx 조회
    int userIdx = handler.getUserIdxFromToken(authHeader);

    // 서비스 로직 실행
    int deleteResult = planService.deletePlan(planIdx, userIdx);

    // 응답 메세지 세팅
    String msg = "";
    switch (deleteResult) {
      case 200 -> msg = "루틴 삭제 완료";
      case 403 -> msg = "작성자가 아닙니다. 재로그인 해주세요.";
      case 410 -> msg = "회원 탈퇴한 계정입니다.";
      case 404 -> msg = "요청하신 루틴이 존재하지 않습니다.";
      case 500 -> msg = "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> msg = "알 수 없는 오류가 발생했습니다.";
    }

    // 응답 메시지 설정
    return ResponseEntity.status(handler.getHttpStatus(deleteResult))
        .body(handler.createResponse(deleteResult, msg));
  }

  // 루틴 시작 API
  @PatchMapping("/auth/stopNgo")
  public ResponseEntity<Map<String, Object>> stopNgo(@RequestHeader("Authorization") String authHeader,
                                                     @Valid @RequestBody PlanRequestDTO requestDTO,
                                                     BindingResult bindingResult) {
    // 유효성 검사 실패 시 에러 반환
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().body(handler.getValidationErrors(bindingResult));
    }

    int planIdx = requestDTO.getPlanIdx();
    // userIdx 조회
    int userIdx = handler.getUserIdxFromToken(authHeader);
    int isActive = requestDTO.getIsActive();
    int isDeleted = 0;

    int result = planService.goStopPlan(planIdx, userIdx, isActive, isDeleted);

    // 응답 메세지 세팅
    String msg = "";
    switch (result) {
      case 200 -> msg = isActive == 1 ? "루틴을 활성화 합니다." : "루틴을 비활성화 합니다.";
      case 403 -> msg = "작성자가 아닙니다. 재로그인 해주세요.";
      case 404 -> msg = "요청하신 루틴이 존재하지 않습니다.";
      case 410 -> msg = "회원 탈퇴한 계정입니다.";
      case 500 -> msg = "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> msg = "알 수 없는 오류가 발생했습니다.";
    }

    // 응답 메시지 설정
    return ResponseEntity.status(handler.getHttpStatus(result))
        .body(handler.createResponse(result, msg));
  }

  // 루틴 추천하기
  @PostMapping("/auth/likePlan/{planIdx}")
  public ResponseEntity<Map<String, Object>> likePlan(@RequestHeader("Authorization") String authHeader,
                                                      @PathVariable int planIdx) {
    int isDeleted = 0;

    // userIdx 조회
    int userIdx = handler.getUserIdxFromToken(authHeader);

    int result = planService.likePlan(planIdx, userIdx, isDeleted);

    // 응답 메세지 세팅
    String msg = "";
    switch (result) {
      case 200 -> msg = "루틴을 추천합니다.";
      case 404 -> msg = "요청하신 루틴이 존재하지 않습니다.";
      case 409 -> msg = "이미 추천한 루틴입니다.";
      case 410 -> msg = "회원 탈퇴한 계정입니다.";
      case 500 -> msg = "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> msg = "알 수 없는 오류가 발생했습니다.";
    }

    // 응답 메시지 설정
    return ResponseEntity.status(handler.getHttpStatus(result))
        .body(handler.createResponse(result, msg));
  }

  // 추천 여부 조회
  @GetMapping("/checkLike/{planIdx}")
  public ResponseEntity<Map<String, Object>> checkLike(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                       @PathVariable int planIdx) {

    int userIdx = 0; // 기본값: 비로그인

    // 토큰이 존재하고 만료되지 않은 경우에만 userIdx 추출
    if (authHeader != null && !handler.validToken(authHeader)) {
      userIdx = handler.getUserIdxFromToken(authHeader);
    }

    boolean result = planService.checkLike(planIdx, userIdx);

    return ResponseEntity.status(handler.getHttpStatus(200))
        .body(handler.createResponse(200, result));
  }

  // 루틴 추천 취소
  @DeleteMapping("/auth/unLikePlan/{planIdx}")
  public ResponseEntity<Map<String, Object>> unLikePlan(@RequestHeader("Authorization") String authHeader,
                                                        @PathVariable int planIdx) {
    // userIdx 조회
    int userIdx = handler.getUserIdxFromToken(authHeader);

    int result = planService.unLikePlan(planIdx, userIdx);

    // 응답 메세지 세팅
    String msg = "";
    switch (result) {
      case 200 -> msg = "루틴 추천을 취소 합니다.";
      case 404 -> msg = "루틴이 존재하지 않거나, 이미 추천을 취소 했습니다.";
      case 410 -> msg = "회원 탈퇴한 계정입니다.";
      case 500 -> msg = "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> msg = "알 수 없는 오류가 발생했습니다.";
    }

    // 응답 메시지 설정
    return ResponseEntity.status(handler.getHttpStatus(result))
        .body(handler.createResponse(result, msg));
  }

  // 조기 완료
  @PatchMapping("/auth/earlyComplete/{planIdx}")
  public ResponseEntity<Map<String, Object>> earlyComplete(@RequestHeader("Authorization") String authHeader,
                                                           @PathVariable int planIdx) {
    // userIdx 조회
    int userIdx = handler.getUserIdxFromToken(authHeader);

    int result = planService.updateEarlyComplete(planIdx, userIdx);

    // 응답 메세지 세팅
    String msg = "";
    switch (result) {
      case 200 -> msg = "조기 종료 되었습니다.";
      case 403 -> msg = "작성자가 아닙니다. 재로그인 해주세요.";
      case 404 -> msg = "요청하신 루틴이 존재하지 않거나, 삭제 처리 된 루틴입니다.";
      case 409 -> msg = "이미 완료 처리된 루틴입니다.";
      case 410 -> msg = "회원 탈퇴한 계정입니다.";
      case 412 -> msg = "활성화 된 루틴이 아닙니다. 루틴 활성화 후 실행해 주세요.";
      case 500 -> msg = "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> msg = "알 수 없는 오류가 발생했습니다.";
    }

    // 응답 메시지 설정
    return ResponseEntity.status(handler.getHttpStatus(result))
        .body(handler.createResponse(result, msg));
  }

  // 후기 작성
  @PatchMapping("/auth/addReview")
  public ResponseEntity<Map<String, Object>> addreview(@RequestHeader("Authorization") String authHeader,
                                                       @RequestBody PlanRequestDTO requestDTO) {
    // userIdx 조회
    int userIdx = handler.getUserIdxFromToken(authHeader);
    requestDTO.setUserIdx(userIdx);

    int result = planService.addReview(requestDTO);

    // 응답 메세지 세팅
    String msg = "";
    switch (result) {
      case 200 -> msg = "후기가 정상적으로 저장 되었습니다.";
      case 403 -> msg = "작성자가 아닙니다. 재로그인 해주세요.";
      case 404 -> msg = "요청하신 루틴이 존재하지 않거나, 삭제 처리 된 루틴입니다.";
      case 409 -> msg = "이미 후기를 작성했습니다. 수정을 원할 경우, 수정 api를 사용해주세요.";
      case 410 -> msg = "회원 탈퇴한 계정입니다.";
      case 412 -> msg = "후기를 작성 하기 전 루틴을 완료해야 합니다.";
      case 500 -> msg = "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> msg = "알 수 없는 오류가 발생했습니다.";
    }

    // 응답 메시지 설정
    return ResponseEntity.status(handler.getHttpStatus(result))
        .body(handler.createResponse(result, msg));
  }

  // 후기 수정
  @PatchMapping("/auth/modifyReview")
  public ResponseEntity<Map<String, Object>> modifyReview(@RequestHeader("Authorization") String authHeader,
                                                          @RequestBody PlanRequestDTO requestDTO) {
    // userIdx 조회
    int userIdx = handler.getUserIdxFromToken(authHeader);
    requestDTO.setUserIdx(userIdx);

    int result = planService.modifyReview(requestDTO);

    // 응답 메세지 세팅
    String msg = "";
    switch (result) {
      case 200 -> msg = "후기가 정상적으로 수정 되었습니다.";
      case 403 -> msg = "작성자가 아닙니다. 재로그인 해주세요.";
      case 404 -> msg = "요청하신 루틴이 존재하지 않거나, 삭제 처리 된 루틴입니다.";
      case 410 -> msg = "회원 탈퇴한 계정입니다.";
      case 412 -> msg = "후기를 수정 하기 전 루틴을 완료하거나 후기를 작성해야 합니다.";
      case 500 -> msg = "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> msg = "알 수 없는 오류가 발생했습니다.";
    }

    // 응답 메시지 설정
    return ResponseEntity.status(handler.getHttpStatus(result))
        .body(handler.createResponse(result, msg));
  }

}
