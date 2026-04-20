package com.godLife.project.controller.v1;

import com.godLife.project.dto.model.content.FaQDTO;
import com.godLife.project.dto.query.search.SearchQueryDTO;
import com.godLife.project.dto.query.content.FaqListDTO;
import com.godLife.project.dto.response.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.godLife.project.service.interfaces.FaqService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/faq")
public class FaqController {
  private final FaqService faqService;

  public FaqController(FaqService faqService) {
    this.faqService = faqService;
  }

  // FAQ 조회
  @GetMapping
  public ResponseEntity<?> getAllFaq() {
    List<FaqListDTO> faqList = faqService.selectAllFaq();

    // FAQ 게시글이 없을 경우 404 상태 코드와 메세지 반환
    if (faqList.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(ApiResponse.of(404, "게시글이 존재하지 않습니다."));
    }

    return ResponseEntity.ok(ApiResponse.of(200, "FAQ 전체 조회 성공", faqList));
  }

  // FAQ 카테고리 조회
  @GetMapping("category/{faqCategory}")
  public ResponseEntity<?> selectCateFaq(@PathVariable Integer faqCategory) {
    List<FaqListDTO> faqList = faqService.selectCateFaq(faqCategory);

    // FAQ 게시글이 없을 경우 404 상태 코드와 메세지 반환
    if (faqList.isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(ApiResponse.of(404, "게시글이 존재하지 않습니다."));
    }

    return ResponseEntity.ok(ApiResponse.of(200, "카테고리별 FAQ 조회 성공", faqList));
  }


  // FAQ 상세 조회
  @GetMapping("/{faqIdx}")
  public ResponseEntity<?> getFaqById(@PathVariable Integer faqIdx) {
    // faqIdx가 null인 경우를 체크하여 처리
    if (faqIdx == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.of(404, "FAQ가 없습니다."));
    }

    FaQDTO faq = faqService.getFaqById(faqIdx);

    if (faq == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.of(404, "FAQ를 찾을 수 없습니다."));
    }

    // 정상적인 경우 응답
    return ResponseEntity.ok(ApiResponse.of(200, "FAQ 상세 조회 성공", faq));
  }

@PostMapping("/admin/write")
  public ResponseEntity createFaq(
          @RequestBody FaQDTO faq){
    int result = faqService.createFaq(faq);

    // 성공 / 실패 응답
  return ResponseEntity.status(result).body(result == 200 ? "FAQ 등록 성공" : "FAQ 등록 실패");
}

@PatchMapping("/admin/{faqIdx}")
  public ResponseEntity<String> updateFaq(@PathVariable int faqIdx, @RequestBody FaQDTO faqDTO) {
  // faqIdx를 DTO set하여 전달
  faqDTO.setFaqIdx(faqIdx);

  // 서비스 호출
  int result = faqService.updateFaq(faqDTO);

  // 상태 코드 및 메시지 반환
  switch (result) {
    case 200:
      return ResponseEntity.ok("FAQ 수정 성공");
    case 500:
      return ResponseEntity.status(500).body("서버 오류로 인해 FAQ 수정에 실패했습니다.");
    default:
      return ResponseEntity.status(500).body("알 수 없는 오류가 발생했습니다.");
    }
  }

  @DeleteMapping("/admin/{faqIdx}")
  public ResponseEntity<String> deleteFaq(@PathVariable int faqIdx){
    // 호출
    int result = faqService.deleteFaq(faqIdx);

    // 상태 코드 및 메시지 반환
    switch (result) {
      case 200:
        return ResponseEntity.ok("FAQ 삭제 성공");
      case 500:
        return ResponseEntity.status(500).body("서버 오류로 인해 FAQ 삭제에 실패했습니다.");
      default:
        return ResponseEntity.status(500).body("알 수 없는 오류가 발생했습니다.");
    }
  }

  // FAQ 검색
  @GetMapping("/search")
  public ResponseEntity<List<FaQDTO>> searchFaq(@ModelAttribute SearchQueryDTO searchQuery) {
    List<FaQDTO> result = faqService.searchFaq(searchQuery);
    return ResponseEntity.ok(result);
  }
}
