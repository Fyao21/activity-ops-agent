package com.example.activityagent.client;

import com.example.activityagent.common.BusinessException;
import com.example.activityagent.config.AgentProperties;
import com.example.activityagent.dto.AgentQueryRequest;
import com.example.activityagent.vo.AgentQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonAgentClient {

    private final RestTemplate restTemplate;
    private final AgentProperties agentProperties;

    public AgentQueryResponse query(AgentQueryRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AgentQueryRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<AgentQueryResponse> response = restTemplate.exchange(
                agentProperties.getPythonUrl(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                }
            );
            AgentQueryResponse body = response.getBody();
            if (body == null) {
                throw new BusinessException("Python Agent returned empty response");
            }
            return body;
        } catch (RestClientException ex) {
            log.error("Call Python Agent failed", ex);
            throw new BusinessException("Call Python Agent service failed");
        }
    }
}
