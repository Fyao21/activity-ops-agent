package com.example.activityagent.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;
    private String requestId;
    private Long timestamp;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = ErrorCode.SUCCESS.getCode();
        r.message = ErrorCode.SUCCESS.getMessage();
        r.data = data;
        r.requestId = UUID.randomUUID().toString().substring(0, 8);
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = success(data);
        r.message = message;
        return r;
    }

    public static <T> Result<T> fail(String message) {
        Result<T> r = new Result<>();
        r.code = ErrorCode.SYSTEM_ERROR.getCode();
        r.message = message;
        r.data = null;
        r.requestId = UUID.randomUUID().toString().substring(0, 8);
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        Result<T> r = new Result<>();
        r.code = errorCode.getCode();
        r.message = errorCode.getMessage();
        r.data = null;
        r.requestId = UUID.randomUUID().toString().substring(0, 8);
        r.timestamp = System.currentTimeMillis();
        return r;
    }
}
