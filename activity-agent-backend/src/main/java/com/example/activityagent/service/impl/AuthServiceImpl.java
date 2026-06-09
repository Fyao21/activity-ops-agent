package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.dto.LoginRequest;
import com.example.activityagent.entity.SysUser;
import com.example.activityagent.mapper.SysUserMapper;
import com.example.activityagent.service.AuthService;
import com.example.activityagent.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Authenticate user by username + password using BCrypt comparison.
     * On success, create a session token stored in Redis with 12h TTL.
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, request.getUsername())
            .last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> cacheValue = new HashMap<>();
        cacheValue.put("userId", user.getId());
        cacheValue.put("username", user.getUsername());
        cacheValue.put("role", user.getRole());
        redisTemplate.opsForValue().set("login:token:" + token, cacheValue, Duration.ofHours(12));
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    }
}
