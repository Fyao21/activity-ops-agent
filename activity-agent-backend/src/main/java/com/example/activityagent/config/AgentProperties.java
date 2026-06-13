package com.example.activityagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private String pythonBaseUrl = "http://localhost:8000";
    private String pythonEndpoint = "/agent/query";
    private Integer connectTimeoutMs = 5000;
    private Integer readTimeoutMs = 60000;

    /**
     * Compose full URL from base URL + endpoint, avoiding path duplication.
     */
    public String getFullUrl() {
        String base = pythonBaseUrl;
        String endpoint = pythonEndpoint;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        return base + endpoint;
    }
}
