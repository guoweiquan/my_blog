package com.example.blog.content.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

    private final Long id;
    private final Long postId;
    private final Long parentId;
    private final String content;
    private final String authorName;
    private final LocalDateTime createdAt;
    private final List<CommentResponse> children;
}
