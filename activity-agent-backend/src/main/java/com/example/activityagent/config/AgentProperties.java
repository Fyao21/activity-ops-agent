package com.example.activityagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private String pythonUrl;
    private Integer connectTimeoutMs = 5000;
    private Integer readTimeoutMs = 60000;
}
