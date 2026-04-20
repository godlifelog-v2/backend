package com.godLife.project.jwt;

import com.godLife.project.exception.CustomException;
import com.godLife.project.exception.UnauthorizedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtil {

  private final SecretKey secretKey;

  public JWTUtil(@Value("${spring.jwt.secret}") String secret) {
    secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
  }

  public String getUsername(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
  }

  public String getRole(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
  }

  public String getCategory(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("category", String.class);
  }

  /** null이면 구형 토큰(claim 없음)으로 판단하고 호출자가 DB fallback 처리 */
  public Integer getUserIdx(String token) {
    Object value = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("userIdx");
    if (value == null) return null;
    return ((Number) value).intValue();
  }

  public boolean isExpired(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
  }

  public boolean isTokenExpired(String token) {
    return isExpired(token);
  }

  /** authHeader에서 Bearer 토큰을 추출한다. null이거나 Bearer 접두사가 없으면 401 CustomException */
  public String extractBearerToken(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new CustomException("유효하지 않은 Authorization 헤더입니다.", HttpStatus.UNAUTHORIZED);
    }
    return authHeader.substring(7);
  }

  public String createJwt(String category, String username, int userIdx, String role, Long expiredMs) {
    return Jwts.builder()
        .claim("category", category)
        .claim("username", username)
        .claim("userIdx", userIdx)
        .claim("role", role)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiredMs))
        .signWith(secretKey)
        .compact();
  }

  @Deprecated
  public String createJwt(String category, String username, String role, Long expiredMs) {
    return createJwt(category, username, 0, role, expiredMs);
  }

  public String extractJwt(final StompHeaderAccessor accessor) {
    return accessor.getFirstNativeHeader("Authorization");
  }

  public void validateToken(final String token) {
    try {
      Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
    } catch (SecurityException | MalformedJwtException | SignatureException | WeakKeyException e) {
      throw UnauthorizedException.of(e.getClass().getName(), "잘못된 JWT 서명입니다.");
    } catch (ExpiredJwtException e) {
      throw UnauthorizedException.of(e.getClass().getName(), "만료된 JWT 토큰입니다.");
    } catch (UnsupportedJwtException e) {
      throw UnauthorizedException.of(e.getClass().getName(), "지원되지 않는 JWT 토큰입니다.");
    } catch (IllegalArgumentException e) {
      throw UnauthorizedException.of(e.getClass().getName(), "JWT 토큰이 잘못되었습니다.");
    } catch (JwtException e) {
      throw UnauthorizedException.of(e.getClass().getName(), "JWT 처리 중 오류가 발생했습니다.");
    }
  }

  public Authentication getAuthentication(String token) {
    String username = getUsername(token);
    String role = getRole(token);

    GrantedAuthority authority = new SimpleGrantedAuthority(role);

    return new UsernamePasswordAuthenticationToken(username, null, java.util.Collections.singletonList(authority));
  }
}
