package com.example.activityagent.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(1, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(1, message, data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(0, message, null);
    }
}
