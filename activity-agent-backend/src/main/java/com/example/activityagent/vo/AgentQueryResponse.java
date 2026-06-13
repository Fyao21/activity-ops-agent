package com.example.activityagent.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Agent 查询响应 VO。
 * <p>
 * 序列化（Java → 前端）统一使用 camelCase，与其他端点保持一致；
 * 反序列化（Python → Java）同时接受 snake_case 和 camelCase。
 */
@Data
public class AgentQueryResponse {
    private String question;

    @JsonAlias("generated_sql")
    private String generatedSql;

    @JsonAlias("query_result")
    private List<Map<String, Object>> queryResult;

    private String answer;
    private Boolean success;

    @JsonAlias("error_message")
    private String errorMessage;

    @JsonAlias("risk_level")
    private Integer riskLevel;

    @JsonAlias("risk_reason")
    private String riskReason;
}
