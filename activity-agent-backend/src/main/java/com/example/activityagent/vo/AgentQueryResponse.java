package com.example.activityagent.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;
import java.util.Map;

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
}
