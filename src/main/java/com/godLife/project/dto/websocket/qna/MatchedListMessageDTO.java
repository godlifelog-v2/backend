package com.godLife.project.dto.websocket.qna;

import com.godLife.project.dto.websocket.qna.QnaMatchedListDTO;
import lombok.Data;

import java.util.List;

@Data
public class MatchedListMessageDTO {

  private List<QnaMatchedListDTO> matchedQnA;

  private String status;
  // private String roomNo;

}
