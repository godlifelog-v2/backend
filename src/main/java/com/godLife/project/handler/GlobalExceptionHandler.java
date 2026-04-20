package com.godLife.project.handler;

import com.godLife.project.dto.response.common.ApiResponse;
import com.godLife.project.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.*;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<?>> handleJsonParseException(HttpMessageNotReadableException ex) {
    String cause = ex.getMostSpecificCause().getMessage();
    String message = (cause != null && !cause.isBlank())
        ? "JSON 파싱 오류: " + cause
        : "잘못된 JSON 형식입니다. 필드값이 누락되었거나 오타가 있을 수 있습니다.";
    return ResponseEntity.badRequest().body(ApiResponse.of(400, message));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult().getFieldErrors()
        .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
    return ResponseEntity.badRequest().body(ApiResponse.of(400, "입력값 유효성 검사 실패", errors));
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ApiResponse<?>> handleNoSuchElementException(NoSuchElementException ex) {
    String msg = ex.getMessage() != null ? ex.getMessage() : "요청한 리소스를 찾을 수 없습니다.";
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.of(404, msg));
  }

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(ApiResponse.of(ex.getStatus().value(), ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<?>> handleGeneralException(Exception e, HttpServletRequest request) {
    log.error("Unhandled exception [{}]: {}", request.getRequestURI(), e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.of(500, "서버 내부 오류가 발생했습니다."));
  }
}
