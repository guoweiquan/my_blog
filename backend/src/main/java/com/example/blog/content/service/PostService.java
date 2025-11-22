package com.example.blog.content.service;

import com.example.blog.auth.repository.UserRepository;
import com.example.blog.auth.util.SecurityUtils;
import com.example.blog.common.api.PageResponse;
import com.example.blog.common.enums.ErrorCode;
import com.example.blog.common.exception.BusinessException;
import com.example.blog.common.exception.ResourceNotFoundException;
import com.example.blog.content.dto.PostDetailResponse;
import com.example.blog.content.dto.PostRequest;
import com.example.blog.content.dto.PostSummaryResponse;
import com.example.blog.content.entity.Post;
import com.example.blog.content.entity.Tag;
import com.example.blog.content.repository.PostRepository;
import com.example.blog.content.repository.TagRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
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
    public PostDetailResponse getPublishedPostBySlug(String slug) {
        Post post = postRepository.findBySlugAndDeletedAtIsNull(slug)
                .filter(Post::isPublished)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在或未发布"));
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

    @Transactional
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
        return toDetail(saved);
    }

    @Transactional
    public PostDetailResponse update(Long id, PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        if (!post.getSlug().equals(request.getSlug()) && postRepository.existsBySlug(request.getSlug())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Slug 已存在");
        }
        applyRequest(post, request);
        Post saved = postRepository.save(post);
        return toDetail(saved);
    }

    @Transactional
    public void delete(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    private void applyRequest(Post post, PostRequest request) {
        post.setTitle(request.getTitle());
        post.setSlug(request.getSlug());
        post.setSummary(request.getSummary());
        post.setContent(request.getContent());
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
            return new java.util.HashSet<>();
        }
        List<Tag> tags = tagRepository.findByIdIn(ids);
        if (tags.size() != ids.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "部分标签不存在");
        }
        return new java.util.HashSet<>(tags);
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
                .build();
    }

    private PostDetailResponse toDetail(Post post) {
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
                .build();
    }

    private PageRequest buildPageRequest(int page, int size) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.min(Math.max(size, 1), 50);
        return PageRequest.of(safePage, safeSize);
    }
}
