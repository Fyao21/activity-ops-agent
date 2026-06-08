package com.example.activityagent.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, AgentProperties agentProperties) {
        return builder
            .setConnectTimeout(Duration.ofMillis(agentProperties.getConnectTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(agentProperties.getReadTimeoutMs()))
            .build();
    }
}
