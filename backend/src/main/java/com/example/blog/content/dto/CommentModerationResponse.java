package com.example.blog.content.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentModerationResponse {

    private final Long id;
    private final Long postId;
    private final String postTitle;
    private final String authorName;
    private final String content;
    private final String status;
    private final LocalDateTime createdAt;
}
