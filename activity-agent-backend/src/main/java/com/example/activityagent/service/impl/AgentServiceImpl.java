package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.example.activityagent.client.PythonAgentClient;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.common.ErrorCode;
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
            log.error("Python Agent call failed for user={}, question={}", request.getUserId(), request.getQuestion(), ex);
            saveRecord(request, null, null, null, false, ex.getMessage(), 0, "");
            throw new BusinessException(ErrorCode.AGENT_CALL_FAILED, ex.getMessage());
        }

        saveRecord(
            request,
            response.getGeneratedSql(),
            toJson(response.getQueryResult()),
            response.getAnswer(),
            Boolean.TRUE.equals(response.getSuccess()),
            response.getErrorMessage(),
            response.getRiskLevel() != null ? response.getRiskLevel() : 0,
            response.getRiskReason() != null ? response.getRiskReason() : ""
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
        String errorMessage,
        int riskLevel,
        String riskReason
    ) {
        AgentQaRecord record = new AgentQaRecord();
        record.setUserId(request.getUserId());
        record.setQuestion(request.getQuestion());
        record.setGeneratedSql(generatedSql);
        record.setQueryResult(queryResult);
        record.setAnswer(answer);
        record.setSuccess(success ? 1 : 0);
        record.setErrorMessage(errorMessage);
        record.setRiskLevel(riskLevel);
        record.setRiskReason(riskReason);
        agentQaRecordMapper.insert(record);
    }

    private String toJson(Object data) {
        if (data == null) return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            log.warn("Serialize query result failed", ex);
            return String.valueOf(data);
        }
    }
}
