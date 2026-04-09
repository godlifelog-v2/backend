package com.godLife.project.dto.websocket.qna;

import com.godLife.project.dto.websocket.qna.QnaWaitListDTO;
import lombok.Data;

import java.util.List;

@Data
public class WaitListMessageDTO {

  private List<QnaWaitListDTO> waitQnA;

  private String status;
  // private String roomNo;

}
