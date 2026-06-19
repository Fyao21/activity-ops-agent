package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.example.activityagent.client.PythonAgentClient;
import com.example.activityagent.dto.AgentQueryRequest;
import com.example.activityagent.entity.AgentQaRecord;
import com.example.activityagent.mapper.AgentQaRecordMapper;
import com.example.activityagent.service.AgentService;
import com.example.activityagent.vo.AgentQueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final PythonAgentClient pythonAgentClient;
    private final AgentQaRecordMapper agentQaRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AgentQueryResponse query(AgentQueryRequest request) {
        AgentQueryResponse response;
        try {
            response = pythonAgentClient.query(request);
        } catch (Exception ex) {
            saveRecord(request, null, null, null, null, false, ex.getMessage());
            throw ex;
        }

        normalizeResponse(request, response);
        saveRecord(
            request,
            response.getRouteType(),
            response.getGeneratedSql(),
            toJson(response.getRetrievedChunks()),
            response.getAnswer(),
            Boolean.TRUE.equals(response.getSuccess()),
            response.getErrorMessage()
        );
        return response;
    }

    @Override
    public Boolean deleteRecord(Long id) {
        if (id == null) {
            throw new com.example.activityagent.common.BusinessException("agent QA record id must not be null");
        }
        if (agentQaRecordMapper.selectById(id) == null) {
            throw new com.example.activityagent.common.BusinessException("Agent QA record does not exist: " + id);
        }
        agentQaRecordMapper.deleteById(id);
        return true;
    }

    private void normalizeResponse(AgentQueryRequest request, AgentQueryResponse response) {
        if (StringUtils.isBlank(response.getQuestion())) {
            response.setQuestion(request.getQuestion());
        }
        if (StringUtils.isBlank(response.getRouteType())) {
            response.setRouteType("sql");
        }
        if (StringUtils.isBlank(response.getGeneratedSql())) {
            response.setGeneratedSql("");
        }
        if (response.getQueryResult() == null) {
            response.setQueryResult(Collections.emptyList());
        }
        if (response.getRetrievedChunks() == null) {
            response.setRetrievedChunks(Collections.emptyList());
        }
        if (response.getSuccess() == null) {
            response.setSuccess(false);
        }
    }

    private void saveRecord(
        AgentQueryRequest request,
        String routeType,
        String generatedSql,
        String retrievedContext,
        String answer,
        boolean success,
        String errorMessage
    ) {
        AgentQaRecord record = new AgentQaRecord();
        record.setUserId(request.getUserId());
        record.setCourseId(request.getCourseId());
        record.setQuestion(request.getQuestion());
        record.setRouteType(routeType);
        record.setGeneratedSql(generatedSql);
        record.setRetrievedContext(retrievedContext);
        record.setAnswer(answer);
        record.setSuccess(success ? 1 : 0);
        record.setErrorMessage(errorMessage);
        agentQaRecordMapper.insert(record);
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data == null ? Collections.emptyList() : data);
        } catch (JsonProcessingException ex) {
            log.warn("Serialize retrieved context failed", ex);
            return String.valueOf(data);
        }
    }
}
