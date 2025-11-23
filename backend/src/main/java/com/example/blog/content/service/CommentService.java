package com.example.blog.content.service;

import com.example.blog.auth.repository.UserRepository;
import com.example.blog.auth.util.SecurityUtils;
import com.example.blog.common.api.PageResponse;
import com.example.blog.common.enums.ErrorCode;
import com.example.blog.common.exception.BusinessException;
import com.example.blog.common.exception.ResourceNotFoundException;
import com.example.blog.common.service.AuditLogService;
import com.example.blog.common.service.SensitiveWordFilter;
import com.example.blog.common.util.ContentSanitizer;
import com.example.blog.content.dto.CommentModerationResponse;
import com.example.blog.content.dto.CommentRequest;
import com.example.blog.content.dto.CommentResponse;
import com.example.blog.content.entity.Comment;
import com.example.blog.content.entity.Post;
import com.example.blog.content.repository.CommentRepository;
import com.example.blog.content.repository.PostRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_REJECTED = "rejected";

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ContentSanitizer contentSanitizer;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Transactional(readOnly = true)
    public List<CommentResponse> listApproved(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(postId, STATUS_APPROVED);
        Map<Long, CommentResponse> mapped = new HashMap<>();
        List<CommentResponse> roots = new ArrayList<>();
        for (Comment comment : comments) {
            CommentResponse response = toResponse(comment, new ArrayList<>());
            mapped.put(comment.getId(), response);
            if (comment.getParent() != null) {
                CommentResponse parent = mapped.get(comment.getParent().getId());
                if (parent != null) {
                    parent.getChildren().add(response);
                }
            } else {
                roots.add(response);
            }
        }
        return roots;
    }

    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request, String ip, String userAgent) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录后再评论");
        }
        sensitiveWordFilter.assertClean(request.getContent());
        String sanitizedContent = contentSanitizer.sanitize(request.getContent());
        Comment comment = Comment.builder()
                .post(post)
                .user(userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("用户不存在")))
                .content(sanitizedContent)
                .status(STATUS_PENDING)
                .ipAddress(ip)
                .userAgent(userAgent)
                .build();
        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("父评论不存在"));
            if (!parent.getPost().getId().equals(postId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "父评论不属于当前文章");
            }
            comment.setParent(parent);
        }
        Comment saved = commentRepository.save(comment);
        auditLogService.record("CREATE_COMMENT", "Comment", saved.getId(), Map.of("postId", postId));
        return toResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentModerationResponse> listForModeration(String status, int page, int size) {
        Page<Comment> pager = commentRepository.findByStatus(status != null ? status : STATUS_PENDING,
                PageRequest.of(Math.max(page - 1, 0), Math.min(size, 50)));
        List<CommentModerationResponse> records = pager.getContent().stream()
                .map(comment -> CommentModerationResponse.builder()
                        .id(comment.getId())
                        .postId(comment.getPost().getId())
                        .postTitle(comment.getPost().getTitle())
                        .authorName(comment.getUser() != null
                                ? (comment.getUser().getNickname() != null ? comment.getUser().getNickname() : comment.getUser().getUsername())
                                : (comment.getAuthorName() != null ? comment.getAuthorName() : "匿名用户"))
                        .content(comment.getContent())
                        .status(comment.getStatus())
                        .createdAt(comment.getCreatedAt())
                        .build())
                .toList();
        return PageResponse.<CommentModerationResponse>builder()
                .records(records)
                .page(page)
                .size(size)
                .total(pager.getTotalElements())
                .build();
    }

    @Transactional
    public void approve(Long id) {
        updateStatus(id, STATUS_APPROVED);
    }

    @Transactional
    public void reject(Long id) {
        updateStatus(id, STATUS_REJECTED);
    }

    @Transactional
    public void delete(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));
        Long postId = comment.getPost().getId();
        commentRepository.delete(comment);
        refreshCommentCount(postId);
        auditLogService.record("DELETE_COMMENT", "Comment", id, Map.of("postId", postId));
    }

    private void updateStatus(Long id, String status) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));
        comment.setStatus(status);
        commentRepository.save(comment);
        refreshCommentCount(comment.getPost().getId());
        auditLogService.record("COMMENT_" + status.toUpperCase(), "Comment", id, Map.of("postId", comment.getPost().getId()));
    }

    private void refreshCommentCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        Long count = commentRepository.countByPostIdAndStatus(postId, STATUS_APPROVED);
        post.setCommentCount(count != null ? count.intValue() : 0);
        postRepository.save(post);
    }

    private CommentResponse toResponse(Comment comment, List<CommentResponse> children) {
        String author = comment.getUser() != null
                ? (comment.getUser().getNickname() != null ? comment.getUser().getNickname() : comment.getUser().getUsername())
                : (comment.getAuthorName() != null ? comment.getAuthorName() : "匿名用户");
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .content(comment.getContent())
                .authorName(author)
                .createdAt(comment.getCreatedAt())
                .children(children)
                .build();
    }
}
