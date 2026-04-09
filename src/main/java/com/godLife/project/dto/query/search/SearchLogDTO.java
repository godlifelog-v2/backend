package com.godLife.project.dto.query.search;

import lombok.Data;

@Data
public class SearchLogDTO {
  private int logIdx;
  private String uniqueId;
  private int userIdx;
  private String searchKeyword;
  private int searchCount;
  private int isDeleted;
}
