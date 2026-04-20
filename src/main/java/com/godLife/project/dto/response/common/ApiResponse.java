package com.godLife.project.dto.response.common;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "성공", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> of(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> of(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }

    // 프론트 호환 shim: "status" 필드로 code를 추가 노출. 프론트 마이그레이션 완료 후 제거.
    @JsonProperty("status")
    public int getStatus() { return code; }
}
