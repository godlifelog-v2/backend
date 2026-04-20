package com.godLife.project.controller.v2;

import com.godLife.project.dto.response.plan.v2.MyPlanV2DTO;
import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.service.interfaces.v2.ListServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/list")
public class ListControllerV2 {

  private final ListServiceV2 listServiceV2;

  @GetMapping("/auth/myPlans")
  public ResponseEntity<?> listMyPlans(@AuthenticationPrincipal CustomUserDetails user) {
    try {
      int userIdx = user.getUserIdx();
      List<MyPlanV2DTO> myPlanList = listServiceV2.getMyPlansList(userIdx);

      if (myPlanList == null) {
        log.error("진행/대기중 루틴 리스트 조회 중 서버 오류 발생");
        throw new Exception("서버 내부 오류로 인해 루틴 리스트 조회에 실패했습니다.");
      }
      if (myPlanList.isEmpty()) {
        throw new NoSuchElementException("진행/대기중인 루틴 없음.");
      }
      return ResponseEntity.ok().body(ApiResponse.of(200, "루틴 리스트 조회 성공", myPlanList));

    } catch (NoSuchElementException e) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    } catch (Exception e) {
      String msg = "서버 내부 오류로 인해 루틴 리스트 조회에 실패했습니다.";
      log.error("e: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.of(500, msg));
    }
  }

  @GetMapping("/auth/todayPlans")
  public ResponseEntity<?> listTodayPlans(
          @AuthenticationPrincipal CustomUserDetails user) {
    try {
      int userIdx = user.getUserIdx();
      List<MyPlanV2DTO> todayPlanList = listServiceV2.getTodayPlansList(userIdx);

      if (todayPlanList == null) {
        log.error("오늘의 루틴 리스트 조회 중 서버 오류 발생");
        throw new Exception("서버 내부 오류로 인해 오늘의 루틴 조회에 실패했습니다.");
      }
      if (todayPlanList.isEmpty()) {
        throw new NoSuchElementException("오늘 진행할 루틴 없음.");
      }

      return ResponseEntity.ok().body(ApiResponse.of(200, "오늘의 루틴 조회 성공", todayPlanList));

    } catch (NoSuchElementException e) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    } catch (Exception e) {
      String msg = "서버 내부 오류로 인해 오늘의 루틴 조회에 실패했습니다.";
      log.error("e: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.of(500, msg));
    }
  }

  /* -----------------------------------------// 함수 구현 //------------------------------------------------------- */

  /* --------------------------------------------------------------------------------------------------------------- */
}
