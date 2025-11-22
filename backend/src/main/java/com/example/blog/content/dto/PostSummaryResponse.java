package com.example.blog.content.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSummaryResponse {

    private final Long id;
    private final String title;
    private final String slug;
    private final String summary;
    private final String coverUrl;
    private final String status;
    private final String authorName;
    private final LocalDateTime publishedAt;
    private final List<String> tagNames;
}
