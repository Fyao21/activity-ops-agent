package com.example.activityagent.client;

import com.example.activityagent.common.BusinessException;
import com.example.activityagent.config.AgentProperties;
import com.example.activityagent.vo.KnowledgeQueryResponse;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonRagClient {

    private final RestTemplate restTemplate;
    private final AgentProperties agentProperties;

    public RagIndexResponse index(Long documentId, String filePath, String fileName) {
        RagIndexRequest request = new RagIndexRequest();
        request.setDocumentId(documentId);
        request.setFilePath(filePath);
        request.setFileName(fileName);
        return post(agentProperties.getRagIndexUrl(), request, new ParameterizedTypeReference<>() {
        });
    }

    public KnowledgeQueryResponse query(String question, Integer topK) {
        RagQueryRequest request = new RagQueryRequest();
        request.setQuestion(question);
        request.setTopK(topK == null ? 4 : topK);
        return post(agentProperties.getRagQueryUrl(), request, new ParameterizedTypeReference<>() {
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

    @Data
    private static class RagIndexRequest {
        @JsonProperty("document_id")
        private Long documentId;
        @JsonProperty("file_path")
        private String filePath;
        @JsonProperty("file_name")
        private String fileName;
    }

    @Data
    private static class RagQueryRequest {
        private String question;
        @JsonProperty("top_k")
        private Integer topK;
    }

    @Data
    public static class RagIndexResponse {
        private Boolean success;
        @JsonAlias("document_id")
        private Long documentId;
        @JsonAlias("chunk_count")
        private Integer chunkCount;
        private String message;
        private List<RagChunkInfo> chunks = new ArrayList<>();
    }

    @Data
    public static class RagChunkInfo {
        @JsonAlias("document_id")
        private Long documentId;
        @JsonAlias("chunk_index")
        private Integer chunkIndex;
        private String content;
        @JsonAlias("vector_id")
        private String vectorId;
    }
}
