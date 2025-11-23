package com.example.blog.analytics.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalyticsOverviewResponse {

    private final long todayPv;
    private final long todayUv;
    private final long publishedPosts;
    private final long pendingComments;
    private final List<HotPost> hotPosts;

    @Getter
    @Builder
    public static class HotPost {
        private final Long postId;
        private final String title;
        private final String slug;
        private final Integer viewCount;
        private final long score;
    }
}
