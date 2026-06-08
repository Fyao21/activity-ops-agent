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
            saveRecord(request, null, null, null, false, ex.getMessage());
            throw ex;
        }

        saveRecord(
            request,
            response.getGeneratedSql(),
            toJson(response.getQueryResult()),
            response.getAnswer(),
            Boolean.TRUE.equals(response.getSuccess()),
            response.getErrorMessage()
        );

        if (StringUtils.isBlank(response.getQuestion())) {
            response.setQuestion(request.getQuestion());
        }
        if (response.getQueryResult() == null) {
            response.setQueryResult(Collections.emptyList());
        }
        return response;
    }

    private void saveRecord(
        AgentQueryRequest request,
        String generatedSql,
        String queryResult,
        String answer,
        boolean success,
        String errorMessage
    ) {
        AgentQaRecord record = new AgentQaRecord();
        record.setUserId(request.getUserId());
        record.setQuestion(request.getQuestion());
        record.setGeneratedSql(generatedSql);
        record.setQueryResult(queryResult);
        record.setAnswer(answer);
        record.setSuccess(success ? 1 : 0);
        record.setErrorMessage(errorMessage);
        agentQaRecordMapper.insert(record);
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            log.warn("Serialize query result failed", ex);
            return String.valueOf(data);
        }
    }
}
