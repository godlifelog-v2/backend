package com.godLife.project.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godLife.project.dto.model.user.UserDTO;
import com.godLife.project.dto.response.user.LoginResponseDTO;
import com.godLife.project.service.interfaces.UserService;
import com.godLife.project.service.interfaces.jwtInterface.RefreshService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

  private final AuthenticationManager authenticationManager;

  private final JWTUtil jwtUtil;

  private final RefreshService refreshService;

  private final UserService userService;

  public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshService refreshService, UserService userService) {

    super.setFilterProcessesUrl("/api/v1/user/login");
    this.authenticationManager = authenticationManager;
    this.jwtUtil = jwtUtil;
    this.refreshService = refreshService;
    this.userService = userService;
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

    // JSON 형식으로 데이터 받기
    try {
      // 요청 본문에서 JSON 데이터를 읽어 LoginDTO 객체로 변환
      ObjectMapper objectMapper = new ObjectMapper();
      UserDTO loginDTO = objectMapper.readValue(request.getInputStream(), UserDTO.class);

      String username = loginDTO.getUserId();
      String password = loginDTO.getUserPw();

      // 스프링 시큐리티에서 username과 password를 검증하기 위해서는 token에 담아야 함
      UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password, null);
      return authenticationManager.authenticate(authToken);
    } catch (IOException e) {
      log.error("로그인 중 JSON 파싱 에러: {}", e.getMessage());
      throw new AuthenticationException("Failed to parse JSON request") {};
    }

  }

  //로그인 성공시 실행하는 메소드 (여기서 JWT를 발급하면 됨)
  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {

    //유저 정보
    String username = authentication.getName();

    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
    GrantedAuthority auth = iterator.next();
    String role = auth.getAuthority();

    // 유저 정보 조회
    UserDTO tempUserDTO = userService.findByUserId(username);

    // 정지 유저 차단
    if (tempUserDTO.getIsBanned() == 1) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write("{\"error\": \"정지된 계정입니다.\"}");
      return;
    }

    // 전송할 데이터 DTO
    LoginResponseDTO loginUserDTO = new LoginResponseDTO();
    loginUserDTO.setUserNick(tempUserDTO.getUserNick());
    loginUserDTO.setNickTag(tempUserDTO.getNickTag());
    if (tempUserDTO.getAuthorityIdx() >= 2) {
      loginUserDTO.setRoleStatus(true);
    }

    // Long accessExp = TimeUnit.MINUTES.toMillis(10);     // 10분
    Long accessExp = TimeUnit.MINUTES.toMillis(5);  // 5분
    // Long accessExp = TimeUnit.SECONDS.toMillis(10); // 10초
    Long refreshExp = TimeUnit.HOURS.toMillis(24);  // 24시간

    //토큰 생성
    String access = jwtUtil.createJwt("access", username, role, accessExp);
    String refresh = jwtUtil.createJwt("refresh", username, role, refreshExp);

    // Refresh 토큰 저장
    refreshService.addRefreshToken(username, refresh, refreshExp);

    //응답 설정
    response.setHeader("Authorization", "Bearer " + access);
    response.addCookie(createCookie("refresh", refresh, request));
    response.setStatus(HttpStatus.OK.value());

    // JSON 형태로 응답
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.writeValue(response.getWriter(), loginUserDTO);

  }

  //로그인 실패시 실행하는 메소드
  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 상태 코드 설정
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    // JSON 형식으로 에러 메시지 전송
    response.getWriter().write("{\"error\": \"아이디 혹은 비밀번호가 일치하지 않습니다.\"}");
  }

  private Cookie createCookie(String key, String value, HttpServletRequest request) {

    Cookie cookie = new Cookie(key, value);
    cookie.setMaxAge(24*60*60); // 생명 주기 : 24시간
    cookie.setPath("/");     // 쿠키 적용 범위
    cookie.setHttpOnly(true);

    // 현재 요청이 HTTPS인지 확인하여 Secure 적용
    boolean isSecure = request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    if (isSecure) {
      cookie.setSecure(true);
      cookie.setAttribute("SameSite", "None");
    }

    return cookie;
  }
}
