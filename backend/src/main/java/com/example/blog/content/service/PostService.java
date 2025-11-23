package com.example.blog.content.service;

import com.example.blog.analytics.service.AnalyticsService;
import com.example.blog.auth.repository.UserRepository;
import com.example.blog.auth.util.SecurityUtils;
import com.example.blog.common.api.PageResponse;
import com.example.blog.common.enums.ErrorCode;
import com.example.blog.common.exception.BusinessException;
import com.example.blog.common.exception.ResourceNotFoundException;
import com.example.blog.common.service.AuditLogService;
import com.example.blog.common.service.SensitiveWordFilter;
import com.example.blog.common.util.ContentSanitizer;
import com.example.blog.content.dto.PostDetailResponse;
import com.example.blog.content.dto.PostRequest;
import com.example.blog.content.dto.PostSummaryResponse;
import com.example.blog.content.entity.Post;
import com.example.blog.content.entity.Tag;
import com.example.blog.content.repository.PostRepository;
import com.example.blog.content.repository.TagRepository;
import com.example.blog.interaction.repository.FavoriteRepository;
import com.example.blog.interaction.repository.LikeRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final LikeRepository likeRepository;
    private final FavoriteRepository favoriteRepository;
    private final ContentSanitizer contentSanitizer;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "posts:published", key = "#page + ':' + #size")
    public PageResponse<PostSummaryResponse> listPublishedPosts(int page, int size) {
        Page<Post> pager = postRepository.findByStatusAndDeletedAtIsNull("published", buildPageRequest(page, size));
        List<PostSummaryResponse> records = pager.getContent().stream().map(this::toSummary).toList();
        return PageResponse.<PostSummaryResponse>builder()
                .records(records)
                .page(page)
                .size(size)
                .total(pager.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "posts:detail", key = "#slug")
    public PostDetailResponse getPublishedPostBySlug(String slug) {
        Post post = postRepository.findBySlugAndDeletedAtIsNull(slug)
                .filter(Post::isPublished)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在或未发布"));
        analyticsService.recordPostView(post);
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        return toDetail(post);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> listAdminPosts(int page, int size, String status) {
        Page<Post> pager;
        if (status != null) {
            pager = postRepository.findByStatusAndDeletedAtIsNull(status, buildPageRequest(page, size));
        } else {
            pager = postRepository.findByDeletedAtIsNull(buildPageRequest(page, size));
        }
        List<PostSummaryResponse> records = pager.getContent().stream().map(this::toSummary).toList();
        return PageResponse.<PostSummaryResponse>builder()
                .records(records)
                .page(page)
                .size(size)
                .total(pager.getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> searchPublished(String keyword, int page, int size) {
        if (!StringUtils.hasText(keyword)) {
            return PageResponse.<PostSummaryResponse>builder()
                    .records(List.of())
                    .page(page)
                    .size(size)
                    .total(0)
                    .build();
        }
        Page<Post> pager;
        PageRequest pageRequest = buildPageRequest(page, size);
        try {
            String booleanKeyword = buildBooleanModeKeyword(keyword);
            if (StringUtils.hasText(booleanKeyword)) {
                pager = postRepository.searchPublishedFulltext(booleanKeyword, pageRequest);
                if (pager.isEmpty()) {
                    pager = postRepository.searchPublished(keyword, pageRequest);
                }
            } else {
                pager = postRepository.searchPublished(keyword, pageRequest);
            }
        } catch (DataAccessException ex) {
            pager = postRepository.searchPublished(keyword, pageRequest);
        }
        List<PostSummaryResponse> records = pager.getContent().stream().map(this::toSummary).toList();
        return PageResponse.<PostSummaryResponse>builder()
                .records(records)
                .page(page)
                .size(size)
                .total(pager.getTotalElements())
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = {"posts:published", "posts:detail"}, allEntries = true)
    public PostDetailResponse create(PostRequest request) {
        if (postRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Slug 已存在");
        }
        Post post = new Post();
        applyRequest(post, request);
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        post.setAuthor(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在")));
        Post saved = postRepository.save(post);
        auditLogService.record("CREATE_POST", "Post", saved.getId(), Map.of("title", saved.getTitle()));
        return toDetail(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"posts:published", "posts:detail"}, allEntries = true)
    public PostDetailResponse update(Long id, PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        if (!post.getSlug().equals(request.getSlug()) && postRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Slug 已存在");
        }
        applyRequest(post, request);
        Post saved = postRepository.save(post);
        auditLogService.record("UPDATE_POST", "Post", saved.getId(), Map.of("status", saved.getStatus()));
        return toDetail(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"posts:published", "posts:detail"}, allEntries = true)
    public void delete(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
        auditLogService.record("DELETE_POST", "Post", id, Map.of("title", post.getTitle()));
    }

    private void applyRequest(Post post, PostRequest request) {
        sensitiveWordFilter.assertClean(request.getTitle());
        sensitiveWordFilter.assertClean(request.getSummary());
        sensitiveWordFilter.assertClean(request.getContent());
        post.setTitle(request.getTitle());
        post.setSlug(request.getSlug());
        post.setSummary(contentSanitizer.sanitize(request.getSummary()));
        post.setContent(contentSanitizer.sanitize(request.getContent()));
        post.setCoverUrl(request.getCoverUrl());
        post.setStatus(request.getStatus());
        post.setReadingTime(request.getReadingTime());
        post.setSeoKeywords(request.getSeoKeywords());
        if ("published".equalsIgnoreCase(request.getStatus()) && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        post.setTags(resolveTags(request.getTagIds()));
    }

    private Set<Tag> resolveTags(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Tag> tags = tagRepository.findByIdIn(ids);
        if (tags.size() != ids.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "部分标签不存在");
        }
        return new HashSet<>(tags);
    }

    private PostSummaryResponse toSummary(Post post) {
        return PostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .summary(post.getSummary())
                .coverUrl(post.getCoverUrl())
                .status(post.getStatus())
                .authorName(post.getAuthor().getNickname() != null ? post.getAuthor().getNickname() : post.getAuthor().getUsername())
                .publishedAt(post.getPublishedAt())
                .tagNames(post.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .build();
    }

    private PostDetailResponse toDetail(Post post) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean liked = userId != null && likeRepository.existsByUserIdAndPostId(userId, post.getId());
        boolean favorited = userId != null && favoriteRepository.existsByUserIdAndPostId(userId, post.getId());
        return PostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .summary(post.getSummary())
                .content(post.getContent())
                .coverUrl(post.getCoverUrl())
                .status(post.getStatus())
                .authorName(post.getAuthor().getNickname() != null ? post.getAuthor().getNickname() : post.getAuthor().getUsername())
                .publishedAt(post.getPublishedAt())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .tagNames(post.getTags().stream().map(Tag::getName).collect(Collectors.toList()))
                .likedByCurrentUser(liked)
                .favoritedByCurrentUser(favorited)
                .build();
    }

    private String buildBooleanModeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return Arrays.stream(keyword.trim().split("\\s+"))
                .filter(StringUtils::hasText)
                .map(word -> word + "*")
                .collect(Collectors.joining(" "));
    }

    private PageRequest buildPageRequest(int page, int size) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.min(Math.max(size, 1), 50);
        return PageRequest.of(safePage, safeSize);
    }
}
