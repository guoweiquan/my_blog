package com.example.blog.analytics.controller;

import com.example.blog.analytics.dto.AnalyticsOverviewResponse;
import com.example.blog.analytics.service.AnalyticsService;
import com.example.blog.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ApiResponse<AnalyticsOverviewResponse> overview() {
        return ApiResponse.success(analyticsService.getOverview());
    }
}
