package com.godLife.project.controller.v1;

import com.godLife.project.dto.response.plan.MyPlanDTO;
import com.godLife.project.dto.query.plan.PlanListDTO;
import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.service.interfaces.ListService;
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
@RequestMapping("/api/v1/list")
public class ListController {

  private final ListService listService;

  @GetMapping("/auth/myPlans")
  public ResponseEntity<?> listMyPlans(@AuthenticationPrincipal CustomUserDetails user) {
    try {
      // userIdx 조회
      int userIdx = user.getUserIdx();
      // 루틴 리스트 조회
      List<MyPlanDTO> myPlanList = listService.getMyPlansList(userIdx);

      if (myPlanList == null) {
        log.error("진행/대기중 루틴 리스트 조회 중 서버 오류 발생");
        throw new Exception("서버 내부 오류로 인해 루틴 리스트 조회에 실패했습니다.");
      }
      if (myPlanList.isEmpty()) {
        throw new NoSuchElementException("진행/대기중인 루틴 없음.");
      }
      // 응답 메시지 설정
      return ResponseEntity.ok().body(ApiResponse.of(200, "루틴 리스트 조회 성공", myPlanList));

    } catch (NoSuchElementException e) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    } catch (Exception e) {
      String msg = "서버 내부 오류로 인해 루틴 리스트 조회에 실패했습니다.";
      log.error("e: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.of(500, msg));
    }
  }

  @GetMapping("/plan/{mode}")
  public ResponseEntity<?> listAllPlans(@PathVariable String mode,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(defaultValue = "0") int status,
                                                          @RequestParam(required = false) List<Integer> target,
                                                          @RequestParam(required = false) List<Integer> job,
                                                          @RequestParam(defaultValue = "latest") String sort,
                                                          @RequestParam(defaultValue = "desc") String order,
                                                          @RequestParam(required = false) String search) {

    //System.out.println("--컨트롤러--");
    //System.out.println(page + " " +  size + " " + status + " " + target + " " + job + " " + sort + " " + order);

    Map<String, Object> response = listService.getAllPlansList(mode, page - 1, size, status, target, job, sort, order, search, 0);

    Object plans = response.get("plans");

    if (plans instanceof List<?>) {
      List<PlanListDTO> tempList = ((List<?>) plans).stream()
          .filter(PlanListDTO.class::isInstance)
          .map(PlanListDTO.class::cast)
          .toList();
      if (tempList.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
      }
    }

    return ResponseEntity.ok(response);
  }

  // 문의 리스트 조회
  @GetMapping("/auth/qna")
  public ResponseEntity<?> listMyQna(@AuthenticationPrincipal CustomUserDetails user,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(defaultValue = "all") String status,
                                                       @RequestParam(defaultValue = "answer") String sort,
                                                       @RequestParam(defaultValue = "desc") String order,
                                                       @RequestParam(required = false) String search) {
    int userIdx = user.getUserIdx();

    Map<String, Object> response = listService.getQnaList(userIdx, page - 1, size, status, sort, order, search);

    return ResponseEntity.ok(response);
  }

  /* -----------------------------------------// 함수 구현 //------------------------------------------------------- */

  /* --------------------------------------------------------------------------------------------------------------- */
}
