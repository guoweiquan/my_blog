package com.example.blog.analytics.service;

import com.example.blog.analytics.dto.AnalyticsOverviewResponse;
import com.example.blog.analytics.dto.AnalyticsOverviewResponse.HotPost;
import com.example.blog.auth.util.SecurityUtils;
import com.example.blog.common.util.RequestUtils;
import com.example.blog.content.dto.PostSummaryResponse;
import com.example.blog.content.entity.Post;
import com.example.blog.content.repository.CommentRepository;
import com.example.blog.content.repository.PostRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final String KEY_PV_DAILY = "pv:daily:";
    private static final String KEY_UV_DAILY = "uv:daily:";
    private static final String KEY_POST_RANKING = "post:ranking:views";

    private final StringRedisTemplate redisTemplate;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPostView(Post post) {
        if (post == null) {
            return;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        String visitor = userId != null ? "user:" + userId : "ip:" + RequestUtils.getClientIp();
        LocalDate today = LocalDate.now();

        redisTemplate.opsForValue().increment(KEY_PV_DAILY + today);
        redisTemplate.expire(KEY_PV_DAILY + today, Duration.ofDays(2));

        redisTemplate.opsForHyperLogLog().add(KEY_UV_DAILY + today, visitor);

        redisTemplate.opsForZSet().incrementScore(KEY_POST_RANKING, post.getId().toString(), 1d);
        postRepository.increaseViewCount(post.getId(), 1L);
    }

    public AnalyticsOverviewResponse getOverview() {
        LocalDate today = LocalDate.now();
        long todayPv = getLongValue(KEY_PV_DAILY + today);
        Long uvSize = redisTemplate.opsForHyperLogLog().size(KEY_UV_DAILY + today);
        long todayUv = uvSize != null ? uvSize : 0L;
        long totalPosts = postRepository.countByStatusAndDeletedAtIsNull("published");
        long pendingComments = commentRepository.countByStatus("pending");

        List<HotPost> hotPosts = buildHotPosts();

        return AnalyticsOverviewResponse.builder()
                .todayPv(todayPv)
                .todayUv(todayUv)
                .publishedPosts(totalPosts)
                .pendingComments(pendingComments)
                .hotPosts(hotPosts)
                .build();
    }

    private long getLongValue(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private List<HotPost> buildHotPosts() {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(KEY_POST_RANKING, 0, 4);
        if (CollectionUtils.isEmpty(tuples)) {
            return List.of();
        }
        List<Long> ids = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(value -> value != null && !value.isBlank())
                .map(Long::valueOf)
                .toList();
        Map<Long, Post> postMap = postRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Post::getId, p -> p));
        List<HotPost> hotPosts = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null) {
                continue;
            }
            Long postId = Long.valueOf(tuple.getValue());
            Post post = postMap.get(postId);
            if (post == null) {
                continue;
            }
            hotPosts.add(HotPost.builder()
                    .postId(postId)
                    .title(post.getTitle())
                    .slug(post.getSlug())
                    .viewCount(post.getViewCount())
                    .score(tuple.getScore() != null ? tuple.getScore().longValue() : 0L)
                    .build());
        }
        return hotPosts;
    }
}
