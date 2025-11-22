package com.example.blog.content.controller;

import com.example.blog.common.api.ApiResponse;
import com.example.blog.common.api.PageResponse;
import com.example.blog.content.dto.PostDetailResponse;
import com.example.blog.content.dto.PostRequest;
import com.example.blog.content.dto.PostSummaryResponse;
import com.example.blog.content.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ApiResponse<PageResponse<PostSummaryResponse>> list(@RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(postService.listPublishedPosts(page, size));
    }

    @GetMapping("/{slug}")
    public ApiResponse<PostDetailResponse> detail(@PathVariable String slug) {
        return ApiResponse.success(postService.getPublishedPostBySlug(slug));
    }

    @GetMapping("/manage")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<PostSummaryResponse>> manage(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "10") int size,
                                                                  @RequestParam(required = false) String status) {
        return ApiResponse.success(postService.listAdminPosts(page, size, status));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PostDetailResponse> create(@Valid @RequestBody PostRequest request) {
        return ApiResponse.success(postService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PostDetailResponse> update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return ApiResponse.success(postService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return ApiResponse.success();
    }
}
