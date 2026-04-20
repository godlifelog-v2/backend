package com.godLife.project.controller.v1;

import com.godLife.project.dto.model.content.ChallengeDTO;
import com.godLife.project.dto.request.challenge.ChallengeJoinRequest;
import com.godLife.project.dto.query.challenge.ChallengeSearchParamDTO;
import com.godLife.project.dto.request.challenge.ChallengeVerifyDTO;
import com.godLife.project.dto.internal.verify.VerifyRecordDTO;
import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.service.interfaces.ChallengeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.godLife.project.dto.security.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/challenges")
public class ChallengeController {

  private final ChallengeService challengeService;

  public ChallengeController(ChallengeService challengeService) {
    this.challengeService = challengeService;
  }


  // -------------------- 최신 챌린지 조회 ----------------
  @GetMapping("/latest")
  public ResponseEntity<?> getLatestChallenges(
          @RequestParam(required = false) String challState,
          @RequestParam(required = false) Integer challCategoryIdx,
          @RequestParam(required = false) String visibilityType,
          @RequestParam(required = false) String challengeType,
          @RequestParam(required = false, defaultValue = "false") Boolean onlyEnded,
          @RequestParam(required = false, defaultValue = "false") Boolean onlyJoined,
          @RequestParam(defaultValue = "1") int page,
          @RequestParam(defaultValue = "10") int size,
          @AuthenticationPrincipal CustomUserDetails user
  ) {
    ChallengeSearchParamDTO param = new ChallengeSearchParamDTO();
    param.setChallState(challState);
    param.setChallCategoryIdx(challCategoryIdx);
    param.setVisibilityType(visibilityType);
    param.setChallengeType(challengeType);
    param.setOnlyEnded(onlyEnded);
    param.setOnlyJoined(onlyJoined);
    param.setPage(page);
    param.setSize(size);

    // 토큰이 있으면 userIdx 추출 (isJoined 표시 + onlyJoined 필터 공통 사용)
    if (user != null) {
      param.setUserIdx((long) user.getUserIdx());
    }

    if (Boolean.TRUE.equals(onlyJoined)) {
      if (param.getUserIdx() == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", 401, "message", "참여중인 챌린지 조회는 로그인이 필요합니다."));
      }
    }

    List<ChallengeDTO> challenges = challengeService.getLatestChallenges(param);
    int totalChallenges = challengeService.countLatestChallenges(param);
    int totalPages = (int) Math.ceil((double) totalChallenges / size);

    Map<String, Object> response = new HashMap<>();
    response.put("status", 200);
    response.put("message", "챌린지 조회 성공");
    response.put("challenges", challenges);
    response.put("totalPages", totalPages);
    response.put("currentPage", page);
    response.put("pageSize", size);

    return ResponseEntity.ok(response);
  }


  // 응답 생성 메서드
  private Map<String, Object> createResponse(int status, String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("status", status);
    response.put("message", message);
    return response;
  }

  // 챌린지 상세 조회
  @GetMapping("/{challIdx}")
  public ResponseEntity<?> getChallengeDetail(
          @PathVariable Long challIdx) {
    Map<String, Object> response = new HashMap<>();

    try {
      ChallengeDTO challengeDetail = challengeService.getChallengeDetail(challIdx);
      response.put("success", true);
      response.put("challenge", challengeDetail);
      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      response.put("success", false);
      response.put("message", e.getMessage());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
  }

  // 인증 기록 조회
  @GetMapping("/verify-records/{challIdx}")
  public ResponseEntity<?> getVerifyRecords(
          @PathVariable Long challIdx,
          @AuthenticationPrincipal CustomUserDetails user) {

    Long userIdx = null;

    if (user != null) {
      userIdx = (long) user.getUserIdx();
    }

    if (userIdx == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
              "success", false,
              "message", "로그인이 필요합니다."
      ));
    }

    List<VerifyRecordDTO> records = challengeService.getVerifyRecords(challIdx, userIdx);

    return ResponseEntity.ok(Map.of(
            "success", true,
            "records", records
    ));
  }


  // 챌린지 참여
  @PostMapping("/auth/join/{challIdx}")
  public ResponseEntity<Object> joinChallenge(@PathVariable Long challIdx,
                                              @AuthenticationPrincipal CustomUserDetails user,
                                              @RequestBody ChallengeJoinRequest joinRequest) {
    try {
      int userIdx = user.getUserIdx();
      ChallengeDTO challenge = challengeService.joinChallenge(
              challIdx,
              userIdx,
              joinRequest.getActivityTime()
      );
      return ResponseEntity.ok(challenge);

    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(ApiResponse.of(400, e.getMessage()));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(ApiResponse.of(400, e.getMessage()));
    } catch (Exception e) {
      log.error("e: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(ApiResponse.of(500, "챌린지 참여 중 오류가 발생했습니다."));
    }
  }


    // 챌린지 인증 (경과 시간 기록)
    @PostMapping("/auth/verify/{challIdx}")
    public ResponseEntity<?> verifyChallenge(
            @PathVariable Long challIdx,
            @RequestBody ChallengeVerifyDTO dto,
            @AuthenticationPrincipal CustomUserDetails user) {

      Map<String, Object> response = new LinkedHashMap<>();
      try {
        int userIdx = user.getUserIdx();

        dto.setChallIdx(challIdx);
        dto.setUserIdx((long) userIdx); // userIdx 세팅 누락 방지

        challengeService.verifyChallenge(dto);

        response.put("status", 200);
        response.put("message", "인증이 완료되었습니다.");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);

      } catch (IllegalArgumentException | IllegalStateException e) {
        response.put("status", 400);
        response.put("error", "Bad Request");
        response.put("message", e.getMessage());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.badRequest().body(response);

      } catch (Exception e) {
        response.put("status", 500);
        response.put("error", "Internal Server Error");
        response.put("message", "서버 오류가 발생했습니다.");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
      }
    }


  // 챌린지 검색 API (제목, 카테고리)
  @GetMapping("/search")
  public List<ChallengeDTO> searchChallenges(
          @RequestParam(required = false) String challTitle,
          @RequestParam(required = false) Integer challCategoryIdx,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "10") int size,
          @RequestParam(defaultValue = "chall_idx") String sort
  ) {
    int offset = page * size;
    return challengeService.searchChallenges(challTitle, challCategoryIdx, offset, size, sort);
  }
}
