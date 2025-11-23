package com.example.blog.content.controller;

import com.example.blog.common.api.ApiResponse;
import com.example.blog.common.api.PageResponse;
import com.example.blog.content.dto.PostSummaryResponse;
import com.example.blog.content.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final PostService postService;

    @GetMapping("/search/posts")
    public ApiResponse<PageResponse<PostSummaryResponse>> searchPosts(@RequestParam("q") String keyword,
                                                                      @RequestParam(defaultValue = "1") int page,
                                                                      @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(postService.searchPublished(keyword, page, size));
    }
}
