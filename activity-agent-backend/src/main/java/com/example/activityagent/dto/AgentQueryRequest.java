package com.example.activityagent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentQueryRequest {
    @NotBlank
    private String question;
    @NotNull
    @JsonProperty("user_id")
    @JsonAlias("userId")
    private Long userId;
}
