package com.example.activityagent.service;

import com.example.activityagent.dto.AgentQueryRequest;
import com.example.activityagent.vo.AgentQueryResponse;

public interface AgentService {
    AgentQueryResponse query(AgentQueryRequest request);
}
