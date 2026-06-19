package com.example.activityagent.client;

import com.example.activityagent.common.BusinessException;
import com.example.activityagent.config.AgentProperties;
import com.example.activityagent.dto.RagDeleteRequest;
import com.example.activityagent.dto.RagDeleteResponse;
import com.example.activityagent.dto.RagIndexRequest;
import com.example.activityagent.dto.RagIndexResponse;
import com.example.activityagent.dto.RagQueryRequest;
import com.example.activityagent.dto.RagQueryResponse;
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
public class PythonRagClient {

    private final RestTemplate restTemplate;
    private final AgentProperties agentProperties;

    public RagIndexResponse index(Long documentId, Long courseId, String filePath, String fileName) {
        RagIndexRequest request = new RagIndexRequest();
        request.setDocumentId(documentId);
        request.setCourseId(courseId);
        request.setFilePath(filePath);
        request.setFileName(fileName);
        return post(agentProperties.getRagIndexUrl(), request, new ParameterizedTypeReference<>() {
        });
    }

    public RagQueryResponse query(Long courseId, String question, Integer topK) {
        RagQueryRequest request = new RagQueryRequest();
        request.setCourseId(courseId);
        request.setQuestion(question);
        request.setTopK(topK == null ? 4 : topK);
        return post(agentProperties.getRagQueryUrl(), request, new ParameterizedTypeReference<>() {
        });
    }

    public RagDeleteResponse delete(Long documentId) {
        RagDeleteRequest request = new RagDeleteRequest();
        request.setDocumentId(documentId);
        return post(agentProperties.getRagDeleteUrl(), request, new ParameterizedTypeReference<>() {
        });
    }

    private <T> T post(String url, Object request, ParameterizedTypeReference<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(request, headers);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            T body = response.getBody();
            if (body == null) {
                throw new BusinessException("Python RAG returned empty response");
            }
            return body;
        } catch (RestClientException ex) {
            log.error("Call Python RAG service failed, url={}", url, ex);
            throw new BusinessException("Call Python RAG service failed");
        }
    }
}
