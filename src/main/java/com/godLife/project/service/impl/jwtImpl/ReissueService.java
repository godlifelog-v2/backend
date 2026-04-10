package com.godLife.project.service.impl.jwtImpl;

import com.godLife.project.dto.model.user.UserDTO;
import com.godLife.project.jwt.JWTUtil;
import com.godLife.project.service.interfaces.UserService;
import com.godLife.project.service.interfaces.jwtInterface.RefreshService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ReissueService {
  private final JWTUtil jwtUtil;
  private final RefreshService refreshService;
  private final UserService userService;

  public ReissueService(JWTUtil jwtUtil, RefreshService refreshService, UserService userService) {
    this.jwtUtil = jwtUtil;
    this.refreshService = refreshService;
    this.userService = userService;
  }

  public ResponseEntity<?> reissueToken(HttpServletRequest request, HttpServletResponse response) {
    // 1. 쿠키에서 refresh 토큰 가져오기
    String refresh = getRefreshTokenFromCookies(request);
    if (refresh == null) {
      log.warn("재발급 토큰 없음");
      response.addCookie(createCookie("refresh", null, 0, request));
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(createErrorResponse("Refresh token is missing", HttpStatus.BAD_REQUEST.value()));
    }

    // 2. refresh 토큰 만료 여부 확인
    try {
      jwtUtil.isExpired(refresh);
    } catch (ExpiredJwtException e) {
      log.warn("재발급 토큰 만료");
      response.addCookie(createCookie("refresh", null, 0, request));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(createErrorResponse("Refresh token is expired", HttpStatus.UNAUTHORIZED.value()));
    }

    // 3. refresh 토큰 카테고리 검증
    if (!"refresh".equals(jwtUtil.getCategory(refresh))) {
      log.warn("재발급 토큰 변조");
      response.addCookie(createCookie("refresh", null, 0, request));
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(createErrorResponse("Invalid refresh token", HttpStatus.FORBIDDEN.value()));
    }

    // 4. DB에 저장되어 있는지 확인
    Boolean isExist = refreshService.existsByRefresh(refresh);
    if (!isExist) {
      log.warn("재발급 토큰 DB에 없음");
      response.addCookie(createCookie("refresh", null, 0, request));
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(createErrorResponse("Refresh token not found in database", HttpStatus.UNAUTHORIZED.value()));
    }

    String username = jwtUtil.getUsername(refresh);
    String role = jwtUtil.getRole(refresh);

    // 5. DB에서 정지 여부 확인 (토큰 재발급 시점에 최신 상태 반영)
    UserDTO user = userService.findByUserId(username);
    if (user.getIsBanned() == 1) {
      log.warn("정지 유저 토큰 재발급 시도: {}", username);
      response.addCookie(createCookie("refresh", null, 0, request));
      refreshService.deleteByRefresh(refresh);
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(createErrorResponse("정지된 계정으로 토큰을 재발급할 수 없습니다.", HttpStatus.FORBIDDEN.value()));
    }

    // Long accessExp = TimeUnit.MINUTES.toMillis(10);     // 10분
    Long accessExp = TimeUnit.MINUTES.toMillis(5);  // 5분
    // Long accessExp = TimeUnit.SECONDS.toMillis(10); // 10초
    Long refreshExp = TimeUnit.HOURS.toMillis(24);  // 24시간

    // 6. 새로운 토큰 생성
    String newAccess = jwtUtil.createJwt("access", username, role, accessExp);
    String newRefresh = jwtUtil.createJwt("refresh", username, role, refreshExp);

    // 기존 Refresh 토큰 삭제 후 새 Refresh 토큰 저장
    refreshService.deleteByRefresh(refresh);
    refreshService.addRefreshToken(username, newRefresh, refreshExp);

    // 7. 응답 헤더에 새로운 access 토큰 추가
    response.setHeader("Authorization", "Bearer " + newAccess);
    response.addCookie(createCookie("refresh", newRefresh, 24*60*60, request));

    return ResponseEntity.ok().body(createSuccessResponse("Token reissued successfully"));
  }

  // 쿠키에서 refresh 토큰 가져오는 메서드
  private String getRefreshTokenFromCookies(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return null;

    for (Cookie cookie : cookies) {
      if ("refresh".equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }

  private Cookie createCookie(String key, String value, int maxAge, HttpServletRequest request) {

    Cookie cookie = new Cookie(key, value);
    cookie.setMaxAge(maxAge);
    cookie.setPath("/");
    cookie.setHttpOnly(true);

    // 현재 요청이 HTTPS인지 확인하여 Secure 적용
    boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    if (isSecure) {
      cookie.setSecure(true);
      cookie.setAttribute("SameSite", "None");
    }

    return cookie;
  }

  private ErrorResponse createErrorResponse(String message, int status) {
    return new ErrorResponse(status, message);
  }

  private SuccessResponse createSuccessResponse(String message) {
    return new SuccessResponse(HttpStatus.OK.value(), message);
  }

  public static class ErrorResponse {
    private final int status;
    private final String message;

    public ErrorResponse(int status, String message) {
      this.status = status;
      this.message = message;
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
  }

  public static class SuccessResponse {
    private final int status;
    private final String message;

    public SuccessResponse(int status, String message) {
      this.status = status;
      this.message = message;
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
  }
}
