package com.example.blog.common.controller;

import com.example.blog.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> payload = Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        );
        return ApiResponse.success(payload);
    }
}
