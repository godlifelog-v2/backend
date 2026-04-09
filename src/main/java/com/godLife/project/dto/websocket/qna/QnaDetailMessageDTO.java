package com.godLife.project.dto.websocket.qna;

import com.godLife.project.dto.websocket.qna.QnaReplyListDTO;
import lombok.Data;

import java.util.List;

@Data
public class QnaDetailMessageDTO {

  private int qnaIdx;

  private String body;

  private String status;

  private List<QnaReplyListDTO> comments;
}
