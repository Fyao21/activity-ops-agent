package com.example.activityagent.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgentQueryResponse {
    private String question;
    @JsonProperty("generated_sql")
    @JsonAlias("generatedSql")
    private String generatedSql;
    @JsonProperty("query_result")
    @JsonAlias("queryResult")
    private List<Map<String, Object>> queryResult;
    private String answer;
    private Boolean success;
    @JsonProperty("error_message")
    @JsonAlias("errorMessage")
    private String errorMessage;

    @JsonProperty("risk_level")
    @JsonAlias("riskLevel")
    private Integer riskLevel;

    @JsonProperty("risk_reason")
    @JsonAlias("riskReason")
    private String riskReason;
}
