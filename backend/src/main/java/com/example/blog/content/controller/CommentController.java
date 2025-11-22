package com.example.blog.content.controller;

import com.example.blog.common.api.ApiResponse;
import com.example.blog.content.dto.CommentRequest;
import com.example.blog.content.dto.CommentResponse;
import com.example.blog.content.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ApiResponse<List<CommentResponse>> list(@PathVariable Long postId) {
        return ApiResponse.success(commentService.listApproved(postId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CommentResponse> create(@PathVariable Long postId,
                                               @Valid @RequestBody CommentRequest request,
                                               HttpServletRequest servletRequest) {
        String ip = servletRequest.getRemoteAddr();
        String ua = servletRequest.getHeader("User-Agent");
        return ApiResponse.success(commentService.addComment(postId, request, ip, ua));
    }
}
