package com.godLife.project.jwt;

import com.godLife.project.dto.security.CustomUserDetails;
import com.godLife.project.dto.model.user.UserDTO;
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

  private static final RequestMatcher matcherAuth   = new AntPathRequestMatcher("/api/v1/*/auth/**");
  private static final RequestMatcher matcherAdmin1 = new AntPathRequestMatcher("/api/v1/*/admin/**");
  private static final RequestMatcher matcherAdmin2 = new AntPathRequestMatcher("/api/v1/admin/**");

  private final JWTUtil jwtUtil;

  public JWTFilter(JWTUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

    if (!(matcherAuth.matches(request) || matcherAdmin1.matches(request) || matcherAdmin2.matches(request))) {
      filterChain.doFilter(request, response);
      return;
    }

    String authorization = request.getHeader("Authorization");

    if (authorization == null || !authorization.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String accessToken = authorization.split(" ")[1];

    // 토큰 만료 여부 확인
    try {
      jwtUtil.isExpired(accessToken);
    } catch (ExpiredJwtException e) {
      sendJson(response, HttpServletResponse.SC_UNAUTHORIZED, "{\"message\": \"access token expired\"}");
      log.error("JWT access 토큰 만료: {}", e.getMessage());
      return;
    }

    // access 토큰 카테고리 확인
    String category = jwtUtil.getCategory(accessToken);
    if (!category.equals("access")) {
      sendJson(response, HttpServletResponse.SC_UNAUTHORIZED, "{\"message\": \"invalid access token\"}");
      log.error("유효하지 않은 access 토큰 사용: {}", category);
      return;
    }

    String username = jwtUtil.getUsername(accessToken);
    String role = jwtUtil.getRole(accessToken);

    UserDTO userDTO = new UserDTO();
    userDTO.setUserId(username);
    userDTO.setAuthorityIdx(Integer.parseInt(role));
    CustomUserDetails customUserDetails = new CustomUserDetails(userDTO);

    Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authToken);

    filterChain.doFilter(request, response);
  }

  private void sendJson(HttpServletResponse response, int status, String json) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setStatus(status);
    PrintWriter writer = response.getWriter();
    writer.print(json);
    writer.flush();
    writer.close();
  }
}
