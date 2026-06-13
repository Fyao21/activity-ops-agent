package com.example.activityagent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Agent 查询请求 DTO。
 * <p>
 * 序列化使用 camelCase（与前端一致）；
 * 反序列化同时接受 snake_case（兼容 Python 端）。
 */
@Data
public class AgentQueryRequest {
    @NotBlank
    private String question;

    @NotNull
    @JsonAlias("user_id")
    private Long userId;
}
