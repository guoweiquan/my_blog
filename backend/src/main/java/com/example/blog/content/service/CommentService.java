package com.example.blog.content.service;

import com.example.blog.auth.repository.UserRepository;
import com.example.blog.auth.util.SecurityUtils;
import com.example.blog.common.enums.ErrorCode;
import com.example.blog.common.exception.BusinessException;
import com.example.blog.common.exception.ResourceNotFoundException;
import com.example.blog.content.dto.CommentRequest;
import com.example.blog.content.dto.CommentResponse;
import com.example.blog.content.entity.Comment;
import com.example.blog.content.entity.Post;
import com.example.blog.content.repository.CommentRepository;
import com.example.blog.content.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CommentResponse> listApproved(Long postId) {
        return commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(postId, "approved").stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request, String ip, String userAgent) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录后再评论");
        }
        Comment comment = Comment.builder()
                .post(post)
                .user(userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("用户不存在")))
                .content(request.getContent())
                .status("approved")
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
        int currentCount = post.getCommentCount() != null ? post.getCommentCount() : 0;
        post.setCommentCount(currentCount + 1);
        postRepository.save(post);
        return toResponse(saved);
    }

    private CommentResponse toResponse(Comment comment) {
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
                .build();
    }
}
