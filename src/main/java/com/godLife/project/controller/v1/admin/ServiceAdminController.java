package com.godLife.project.controller.v1.admin;

import com.godLife.project.dto.websocket.admin.ServiceCenterAdminInfos;
import com.godLife.project.dto.websocket.admin.ServiceCenterAdminList;
import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.enums.WSDestination;
import com.godLife.project.listener.QnaQueueListener;
import com.godLife.project.service.impl.websocketImpl.WebSocketMessageService;
import com.godLife.project.service.interfaces.AdminInterface.serviceCenter.ServiceAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/service/admin")
@Slf4j
@RequiredArgsConstructor
public class ServiceAdminController {

  private final ServiceAdminService serviceAdminService;

  private final QnaQueueListener qnaQueueListener;
  private final WebSocketMessageService messageService;

  // 관리자 상태 조회
  @GetMapping("/get/status")
  public ResponseEntity<?> getStatus(@AuthenticationPrincipal CustomUserDetails user) {
    int userIdx = user.getUserIdx();

    String result = serviceAdminService.getAdminStatus(userIdx);

    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(200, result));
  }

  // 관리자 상태 전환
  @PatchMapping("/switch/status")
  public ResponseEntity<?> switchStatus(@AuthenticationPrincipal CustomUserDetails user) {
    int userIdx = user.getUserIdx();
    String username = user.getUsername();

    String result = serviceAdminService.switchAdminStatus(userIdx);

    if ("활성화".equals(result)) {
      qnaQueueListener.wakeUp(userIdx, username);
    }

    // 클라이언트에게 관리자 목록 전송
    List<ServiceCenterAdminInfos> accessAdminInfos = serviceAdminService.getAllAccessServiceAdminList();

    List<ServiceCenterAdminList> accessAdminList = serviceAdminService.getAccessAdminListForMessage(accessAdminInfos);
    messageService.sendToAll(WSDestination.ALL_ACCESS_ADMIN_LIST.getDestination(), accessAdminList);

    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(200, result));
  }

  // 오토 매칭 쓰레드 강제 활성화
  @PostMapping("/autoMatch/wakeUp")
  public ResponseEntity<?> wakeUpAutoMatch(@AuthenticationPrincipal CustomUserDetails user) {
    String username = user.getUsername();
    int userIdx = user.getUserIdx();

    qnaQueueListener.wakeUp(userIdx, username);

    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.of(200, "QnA 자동 매칭을 깨웠습니다."));
  }

}
