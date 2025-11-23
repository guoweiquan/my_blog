package com.example.blog.interaction.controller;

import com.example.blog.common.api.ApiResponse;
import com.example.blog.interaction.dto.InteractionResponse;
import com.example.blog.interaction.dto.SubscriptionResponse;
import com.example.blog.interaction.service.InteractionService;
import com.example.blog.interaction.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;
    private final SubscriptionService subscriptionService;

    @PostMapping("/posts/{postId}/like")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<InteractionResponse> like(@PathVariable Long postId) {
        return ApiResponse.success(interactionService.toggleLike(postId));
    }

    @PostMapping("/posts/{postId}/favorite")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<InteractionResponse> favorite(@PathVariable Long postId) {
        return ApiResponse.success(interactionService.toggleFavorite(postId));
    }

    @PostMapping("/tags/{tagId}/subscribe")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SubscriptionResponse> subscribe(@PathVariable Long tagId) {
        return ApiResponse.success(subscriptionService.toggle(tagId));
    }
}
