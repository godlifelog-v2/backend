package com.godLife.project.controller.websocket;

import com.godLife.project.dto.request.chat.ChatCreateDTO;
import com.godLife.project.dto.internal.test.TestChatDTO;
import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.service.interfaces.AdminInterface.AdminChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminChatController {

  private final AdminChatService adminChatService;


  // 채팅방 개설
  @PostMapping("/api/v1/admin/chat")
  public ResponseEntity<?> createChatRoom(@AuthenticationPrincipal CustomUserDetails user,
                                                            @RequestBody ChatCreateDTO chatCreateDTO) {

    // 작성자 인덱스 번호 추출
    int adminIdx = user.getUserIdx();
    chatCreateDTO.setCreatedBy(adminIdx);

    // 채팅방 개설 로직 실행
    int roomIdx = adminChatService.createAdminChatRoom(chatCreateDTO);
    return ResponseEntity.ok().body(ApiResponse.of(200, "채팅방이 생성되었습니다.", Map.of("roomIdx", roomIdx)));
  }

  // 채팅 기능
  @MessageMapping("/roomChat/{roomNo}")
  @SendTo("/sub/roomChat/{roomNo}")
  public TestChatDTO broadcasting(final TestChatDTO request,
                                  @DestinationVariable(value = "roomNo") final String chatRoomNo,
                                  Principal principal) {
    request.setRoomNo(chatRoomNo);
    request.setSender(principal.getName());
    log.info("{roomNo : {}, request : {}}", chatRoomNo, request);

    return request;
  }
}
