package com.example.activityagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.activityagent.mapper")
public class ActivityAgentBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityAgentBackendApplication.class, args);
    }
}
