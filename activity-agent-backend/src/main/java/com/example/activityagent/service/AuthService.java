package com.example.activityagent.service;

import com.example.activityagent.dto.LoginRequest;
import com.example.activityagent.vo.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
