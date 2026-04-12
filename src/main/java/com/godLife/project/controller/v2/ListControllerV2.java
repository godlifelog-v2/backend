package com.godLife.project.controller.v2;

import com.godLife.project.dto.response.plan.v2.MyPlanV2DTO;
import com.godLife.project.dto.response.plan.v2.TodayStatsDTO;
import com.godLife.project.handler.GlobalExceptionHandler;
import com.godLife.project.service.interfaces.v2.ListServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/list")
public class ListControllerV2 {

  @Autowired
  private final GlobalExceptionHandler handler;

  private final ListServiceV2 listServiceV2;

  @GetMapping("/auth/myPlans")
  public ResponseEntity<Map<String, Object>> listMyPlans(@RequestHeader("Authorization") String authHeader) {
    try {
      int userIdx = handler.getUserIdxFromToken(authHeader);
      List<MyPlanV2DTO> myPlanList = listServiceV2.getMyPlansList(userIdx);

      if (myPlanList == null) {
        log.error("진행/대기중 루틴 리스트 조회 중 서버 오류 발생");
        throw new Exception("서버 내부 오류로 인해 루틴 리스트 조회에 실패했습니다.");
      }
      if (myPlanList.isEmpty()) {
        throw new NoSuchElementException("진행/대기중인 루틴 없음.");
      }
      return ResponseEntity.ok().body(handler.createResponseWithData(200, "루틴 리스트 조회 성공", myPlanList));

    } catch (NoSuchElementException e) {
      return ResponseEntity.status(handler.getHttpStatus(204)).build();
    } catch (Exception e) {
      String msg = "서버 내부 오류로 인해 루틴 리스트 조회에 실패했습니다.";
      log.error("e: ", e);
      return ResponseEntity.status(handler.getHttpStatus(500)).body(handler.createResponse(500, msg));
    }
  }

  @GetMapping("/auth/todayPlans")
  public ResponseEntity<Map<String, Object>> listTodayPlans(
          @RequestHeader("Authorization") String authHeader) {
    try {
      int userIdx = handler.getUserIdxFromToken(authHeader);
      List<MyPlanV2DTO> todayPlanList = listServiceV2.getTodayPlansList(userIdx);

      if (todayPlanList == null) {
        log.error("오늘의 루틴 리스트 조회 중 서버 오류 발생");
        throw new Exception("서버 내부 오류로 인해 오늘의 루틴 조회에 실패했습니다.");
      }
      if (todayPlanList.isEmpty()) {
        throw new NoSuchElementException("오늘 진행할 루틴 없음.");
      }

      return ResponseEntity.ok().body(handler.createResponseWithData(200, "오늘의 루틴 조회 성공", todayPlanList));

    } catch (NoSuchElementException e) {
      return ResponseEntity.status(handler.getHttpStatus(204)).build();
    } catch (Exception e) {
      String msg = "서버 내부 오류로 인해 오늘의 루틴 조회에 실패했습니다.";
      log.error("e: ", e);
      return ResponseEntity.status(handler.getHttpStatus(500))
          .body(handler.createResponse(500, msg));
    }
  }

  @GetMapping("/auth/todayStats")
  public ResponseEntity<Map<String, Object>> getTodayStats(
          @RequestHeader("Authorization") String authHeader) {
    try {
      int userIdx = handler.getUserIdxFromToken(authHeader);
      TodayStatsDTO stats = listServiceV2.getTodayStats(userIdx);
      return ResponseEntity.ok().body(handler.createResponseWithData(200, "통계 조회 성공", stats));
    } catch (Exception e) {
      String msg = "통계 조회 중 오류가 발생했습니다.";
      log.error("e: ", e);
      return ResponseEntity.status(handler.getHttpStatus(500))
          .body(handler.createResponse(500, msg));
    }
  }

  /* -----------------------------------------// 함수 구현 //------------------------------------------------------- */

  /* --------------------------------------------------------------------------------------------------------------- */
}
