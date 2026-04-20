package com.godLife.project.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.dto.model.user.UserDTO;
import com.godLife.project.mapper.VerifyMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class JWTFilter extends OncePerRequestFilter {

  private static final RequestMatcher matcherAuth    = new AntPathRequestMatcher("/api/v1/*/auth/**");
  private static final RequestMatcher matcherAdmin1  = new AntPathRequestMatcher("/api/v1/*/admin/**");
  private static final RequestMatcher matcherAdmin2  = new AntPathRequestMatcher("/api/v1/admin/**");
  private static final RequestMatcher matcherAuthV2  = new AntPathRequestMatcher("/api/v2/*/auth/**");

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final JWTUtil jwtUtil;
  private final VerifyMapper verifyMapper;

  public JWTFilter(JWTUtil jwtUtil, VerifyMapper verifyMapper) {
    this.jwtUtil = jwtUtil;
    this.verifyMapper = verifyMapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

    if (!(matcherAuth.matches(request) || matcherAdmin1.matches(request) || matcherAdmin2.matches(request) || matcherAuthV2.matches(request))) {
      filterChain.doFilter(request, response);
      return;
    }

    String authorization = request.getHeader("Authorization");

    if (authorization == null || !authorization.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String accessToken = authorization.substring(7);

    // 토큰 만료 여부 확인
    try {
      jwtUtil.isExpired(accessToken);
    } catch (ExpiredJwtException e) {
      sendApiResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "access token expired");
      log.error("JWT access 토큰 만료: {}", e.getMessage());
      return;
    }

    // access 토큰 카테고리 확인
    String category = jwtUtil.getCategory(accessToken);
    if (!"access".equals(category)) {
      sendApiResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid access token");
      log.error("유효하지 않은 access 토큰 사용: {}", category);
      return;
    }

    String username = jwtUtil.getUsername(accessToken);
    String role = jwtUtil.getRole(accessToken);

    // userIdx를 JWT claim에서 읽음. 구형 토큰(claim 없음)이면 DB fallback.
    Integer claimUserIdx = jwtUtil.getUserIdx(accessToken);
    int userIdx;
    if (claimUserIdx == null || claimUserIdx == 0) {
      log.warn("구형 토큰(userIdx claim 없음) - DB fallback 실행, userId: {}", username);
      userIdx = verifyMapper.getUserIdxByUserId(username);
    } else {
      userIdx = claimUserIdx;
    }

    UserDTO userDTO = new UserDTO();
    userDTO.setUserId(username);
    userDTO.setAuthorityIdx(Integer.parseInt(role));
    CustomUserDetails customUserDetails = new CustomUserDetails(userDTO, userIdx);

    Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authToken);

    filterChain.doFilter(request, response);
  }

  private void sendApiResponse(HttpServletResponse response, int code, String message) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setStatus(code);
    PrintWriter writer = response.getWriter();
    writer.print(objectMapper.writeValueAsString(ApiResponse.of(code, message)));
    writer.flush();
    writer.close();
  }
}
