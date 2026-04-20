package com.godLife.project.controller.v1;


import com.godLife.project.dto.model.content.QnaDTO;
import com.godLife.project.dto.model.content.QnaReplyDTO;
import com.godLife.project.dto.query.content.QnaDetailDTO;
import com.godLife.project.dto.websocket.qna.QnaDetailMessageDTO;
import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.enums.MessageStatus;
import com.godLife.project.enums.QnaRedisKey;
import com.godLife.project.enums.QnaStatus;
import com.godLife.project.exception.CustomException;
import com.godLife.project.service.impl.redis.RedisService;
import com.godLife.project.service.interfaces.QnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/qna/auth")
@RequiredArgsConstructor
public class QnaController {

  private final QnaService qnaService;
  private final RedisService redisService;



  // 1:1 문의 작성
  @PostMapping("/create")
  public ResponseEntity<?> createQna(@AuthenticationPrincipal CustomUserDetails user,
                                                       @Valid @RequestBody QnaDTO writeQna,
                                                       BindingResult valid) {
    if (valid.hasErrors()) {
      Map<String, String> errors = new java.util.LinkedHashMap<>();
      valid.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      return ResponseEntity.badRequest().body((Map) errors);
    }

    int userIdx = user.getUserIdx();
    writeQna.setQUserIdx(userIdx);

    qnaService.createQna(writeQna);

    // 응답 메시지 설정
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 1:1 문의 본문 조회
  @GetMapping("/get/just/content/{qnaIdx}")
  public ResponseEntity<?> getJustQnaContent(@PathVariable int qnaIdx) {
    String content = qnaService.getQnaContent(qnaIdx);

    return ResponseEntity.ok().body(ApiResponse.of(200, content));
  }

  // 1:1 문의 답변 달기
  @PostMapping("/comment/reply")
  public ResponseEntity<?> commentReply(@AuthenticationPrincipal CustomUserDetails user,
                                                          @Valid @RequestBody QnaReplyDTO qnaReplyDTO,
                                                          BindingResult valid) {
    if (valid.hasErrors()) {
      Map<String, String> errors = new java.util.LinkedHashMap<>();
      valid.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      return ResponseEntity.badRequest().body((Map) errors);
    }

    int userIdx = user.getUserIdx();
    qnaReplyDTO.setUserIdx(userIdx);

    qnaService.commentReply(qnaReplyDTO);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 1:1 문의 수정
  @PatchMapping("/modify")
  public ResponseEntity<?> modifyQnA(@AuthenticationPrincipal CustomUserDetails user,
                                                       @Valid @RequestBody QnaDTO modifyQna,
                                                       BindingResult valid) {
    if (valid.hasErrors()) {
      Map<String, String> errors = new java.util.LinkedHashMap<>();
      valid.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      return ResponseEntity.badRequest().body((Map) errors);
    }

    int userIdx = user.getUserIdx();
    modifyQna.setQUserIdx(userIdx);

    List<String> setStatus = new ArrayList<>();
    setStatus.add(QnaStatus.WAIT.getStatus());
    setStatus.add(QnaStatus.CONNECT.getStatus());

    // 문의 수정
    qnaService.modifyQnA(modifyQna, setStatus);

    return ResponseEntity.ok().body(ApiResponse.of(200, "문의 수정 완료"));
  }

  // 1:1 답변 수정
  @PatchMapping("/modify/reply")
  public ResponseEntity<?> modifyReply(@AuthenticationPrincipal CustomUserDetails user,
                                                         @Valid @RequestBody QnaReplyDTO modifyReplyDTO,
                                                         BindingResult valid) {
    if (valid.hasErrors()) {
      Map<String, String> errors = new java.util.LinkedHashMap<>();
      valid.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
      return ResponseEntity.badRequest().body((Map) errors);
    }

    int userIdx = user.getUserIdx();
    modifyReplyDTO.setUserIdx(userIdx);

    qnaService.modifyReply(modifyReplyDTO);

    return ResponseEntity.ok().body(ApiResponse.of(200, "답변 수정 완료"));
  }

  // 문의 삭제
  @DeleteMapping("/delete/{qnaIdx}")
  ResponseEntity<?> deleteQnA(@AuthenticationPrincipal CustomUserDetails user,
                                                @PathVariable int qnaIdx) {
    int userIdx = user.getUserIdx();

    qnaService.deleteQna(qnaIdx, userIdx);

    return ResponseEntity.ok().body(ApiResponse.of(200, "문의가 정상적으로 삭제 되었습니다."));
  }

  // 답변 삭제
  @DeleteMapping("/delete/reply/{qnaIdx}")
  ResponseEntity<?> deleteReply(@AuthenticationPrincipal CustomUserDetails user,
                                                  @PathVariable int qnaIdx,
                                                  @RequestParam int qnaReplyIdx) {
    int userIdx = user.getUserIdx();

    qnaService.deleteReply(qnaIdx, qnaReplyIdx, userIdx);

    return ResponseEntity.ok().body(ApiResponse.of(200, "답변이 정상적으로 삭제 되었습니다."));
  }

  // 문의 상세 조회 (답변 까지)
  @GetMapping("/{qnaIdx}")
  ResponseEntity<?> getDetailQna(@AuthenticationPrincipal CustomUserDetails user,
                                                   @PathVariable int qnaIdx) {
    int userIdx = user.getUserIdx();

    if (qnaIdx <= 0) {
      throw new CustomException("문의 인덱스를 선택해주세요. 문의 인덱스는 0 보다 커야 합니다.", HttpStatus.BAD_REQUEST);
    }

    // 상세보기 데이터
    QnaDetailMessageDTO base = qnaService.getQnaDetails(qnaIdx, MessageStatus.RELOAD.getStatus(), userIdx);

    QnaDetailDTO response = qnaService.setQnaDetailForUser(base);

    return ResponseEntity.ok().body(ApiResponse.of(200, "문의 상세 조회 성공", response));
  }

  // 문의 완료 처리 api
  @PatchMapping("/complete/{qnaIdx}")
  public ResponseEntity<?> setComplete(@AuthenticationPrincipal CustomUserDetails user,
                                                         @PathVariable int qnaIdx) {
    int userIdx = user.getUserIdx();

    if (qnaIdx == 0) {
      throw new CustomException("완료 처리 할 문의를 선택 해주세요.", HttpStatus.BAD_REQUEST);
    }
    // 수정할 상태 값
    String setStatus = QnaStatus.COMPLETE.getStatus();

    // 조회할 문의의 상태
    List<String> findStatus = new ArrayList<>();
    findStatus.add(QnaStatus.RESPONDING.getStatus());
    findStatus.add(QnaStatus.SLEEP.getStatus());

    // 문의 상태 수정 (완료 처리)
    qnaService.setQnaStatus(qnaIdx, userIdx, setStatus, findStatus);

    // 완료 처리 할 경우 레디스의 문의 추적 정보 삭제
    redisService.deleteData(QnaRedisKey.QNA_ADMIN_ANSWERED.getKey() + qnaIdx);
    redisService.deleteData(QnaRedisKey.QNA_IS_SLEEP.getKey() + qnaIdx);

    return ResponseEntity.ok().body(ApiResponse.of(200, "해당 문의가 완료 처리 되었습니다."));
  }


}
