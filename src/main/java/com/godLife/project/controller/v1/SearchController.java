package com.godLife.project.controller.v1;

import com.godLife.project.dto.query.search.SearchLogDTO;
import com.godLife.project.dto.response.search.SearchLogsResponseDTO;
import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.service.interfaces.SearchService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

  private final SearchService searchService;

  @GetMapping("/log")
  public ResponseEntity<?> searchLog(HttpServletResponse response, HttpServletRequest request,
                                                       @AuthenticationPrincipal CustomUserDetails user,
                                                       @RequestParam(required = false) String keyword) {
    String uniqueId = UUID.randomUUID().toString();
    String key = "isNotLogin";

    // 로그인 여부 확인
    if (user == null) {
      return handleNotLoggedIn(response, request, key, uniqueId, keyword);
    }

    int userIdx = user.getUserIdx();
    SearchLogDTO searchLogDTO = new SearchLogDTO();
    searchLogDTO.setUserIdx(userIdx);

    return (keyword != null) ? setKeyword(searchLogDTO, String.valueOf(userIdx), keyword) : getKeywords(searchLogDTO);
  }

  @PatchMapping("/log/{logIdx}")
  public ResponseEntity<?> deleteSearchLog(HttpServletResponse response, HttpServletRequest request,
                                                             @AuthenticationPrincipal CustomUserDetails user,
                                                             @PathVariable int logIdx) {
    String key = "isNotLogin";
    SearchLogDTO searchLogDTO = new SearchLogDTO();

    if (logIdx <= 0) {
      return setResponseMessages(422);
    }

    searchLogDTO.setLogIdx(logIdx);

    // 로그인 여부 확인
    if (user == null) {
      Cookie[] cookies = request.getCookies();

      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if (key.equals(cookie.getName())) {
            String uuid = cookie.getValue();
            searchLogDTO.setUniqueId(uuid);

            // isNotLogin 쿠키 있음
            int result = searchService.deleteSearchLog(searchLogDTO);
            return setResponseMessages(result);
          }
        }
      }
      // 헤더도 없고 쿠키도 없음
      int result = 400;
      return setResponseMessages(result);
    }
    // 헤더 있음
    int userIdx = user.getUserIdx();
    searchLogDTO.setUserIdx(userIdx);

    int result = searchService.deleteSearchLog(searchLogDTO);
    return setResponseMessages(result);

  }



  /* -----------------------------------------// 함수 구현 //------------------------------------------------------- */
  // 비로그인 유저 처리 함수
  private ResponseEntity<?> handleNotLoggedIn(HttpServletResponse response, HttpServletRequest request, String key, String uniqueId, String keyword) {
    Cookie[] cookies = request.getCookies();
    SearchLogDTO searchLogDTO = new SearchLogDTO();

    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (key.equals(cookie.getName())) {
          String uuid = cookie.getValue();
          searchLogDTO.setUniqueId(uuid);
          return (keyword != null) ? setKeyword(searchLogDTO, uuid, keyword) : getKeywords(searchLogDTO);
        }
      }
    }

    // 쿠키 생성 및 신규 유저 처리
    Cookie cookie = createCookie(key, uniqueId, request);
    response.addCookie(cookie);
    searchLogDTO.setUniqueId(uniqueId);

    return (keyword != null) ? setKeyword(searchLogDTO, uniqueId, keyword) : getKeywords(searchLogDTO);
  }

  // 쿠키 생성 함수
  private Cookie createCookie(String key, String value, HttpServletRequest request) {
    Cookie cookie = new Cookie(key, value);
    cookie.setMaxAge(60 * 60 * 24 * 365);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setAttribute("SameSite", "None");

    // 현재 요청이 HTTPS인지 확인하여 Secure 적용
    boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    if (isSecure) {
      cookie.setSecure(true);
      cookie.setAttribute("SameSite", "None");
    }
    return cookie;
  }

  // 검색 기록 저장 함수
  private ResponseEntity<?> setKeyword(SearchLogDTO searchLogDTO, String id, String keyword) {
    if (id.isEmpty() || keyword.isEmpty()) {
      int status = id.isEmpty() ? 500 : 204;
      String message = id.isEmpty() ? "고유 아이디를 생성하지 못했습니다." : "검색어가 없어 저장하지 못했습니다.";
      return ResponseEntity.status(HttpStatus.valueOf(status)).body(ApiResponse.of(status, message));
    }

    if (!id.contains("-")) {
      searchLogDTO.setUserIdx(Integer.parseInt(id));
    } else {
      searchLogDTO.setUniqueId(id);
    }

    searchLogDTO.setSearchKeyword(keyword);
    searchService.setSearchLog(searchLogDTO);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // 검색 기록 조회 함수
  private ResponseEntity<?> getKeywords(SearchLogDTO searchLogDTO) {
    List<SearchLogsResponseDTO> searchLogs = searchService.getSearchLogs(searchLogDTO);
    return searchLogs.isEmpty() ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        : ResponseEntity.ok().body(ApiResponse.of(200, "검색 기록 조회 성공", searchLogs));
  }

  // 응답 메세지 설정 함수
  private ResponseEntity<?> setResponseMessages(int result) {
    // 응답 메세지 세팅
    String msg = "";
    switch (result) {
      case 204 -> msg = "기록 삭제 완료";
      case 400 -> msg = "쿠키 혹은 로그인 정보가 없습니다. 로그인 먼저 진행 후, 검색을 통해 기록을 저장해주세요.";
      case 412 -> msg = "이미 삭제 처리되었거나, 본인의 검색 기록이 아닙니다.";
      case 422 -> msg = "어떤 검색 기록을 삭제할 지 선택해주세요.";
      case 500 -> msg = "서버 내부적으로 오류가 발생하여 요청을 수행하지 못했습니다.";
      default -> msg = "알 수 없는 오류가 발생했습니다.";
    }

    // 응답 메시지 설정
    return ResponseEntity.status(HttpStatus.valueOf(result)).body(ApiResponse.of(result, msg));
  }
  /* --------------------------------------------------------------------------------------------------------------- */
}
