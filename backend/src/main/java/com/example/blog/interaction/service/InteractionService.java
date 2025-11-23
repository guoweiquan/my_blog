package com.example.blog.interaction.service;

import com.example.blog.auth.entity.User;
import com.example.blog.auth.repository.UserRepository;
import com.example.blog.auth.util.SecurityUtils;
import com.example.blog.common.enums.ErrorCode;
import com.example.blog.common.exception.BusinessException;
import com.example.blog.common.exception.ResourceNotFoundException;
import com.example.blog.common.service.AuditLogService;
import com.example.blog.interaction.dto.InteractionResponse;
import com.example.blog.interaction.entity.Favorite;
import com.example.blog.interaction.entity.LikeRecord;
import com.example.blog.interaction.repository.FavoriteRepository;
import com.example.blog.interaction.repository.LikeRepository;
import com.example.blog.content.entity.Post;
import com.example.blog.content.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InteractionService {

    private final LikeRepository likeRepository;
    private final FavoriteRepository favoriteRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public InteractionResponse toggleLike(Long postId) {
        User user = requireCurrentUser();
        Post post = requirePost(postId);
        LikeRecord record = likeRepository.findByUserIdAndPostId(user.getId(), postId).orElse(null);
        boolean active;
        if (record != null) {
            likeRepository.delete(record);
            post.setLikeCount(Math.max(0, safeInt(post.getLikeCount()) - 1));
            active = false;
        } else {
            likeRepository.save(LikeRecord.builder().user(user).post(post).build());
            post.setLikeCount(safeInt(post.getLikeCount()) + 1);
            active = true;
        }
        postRepository.save(post);
        long total = likeRepository.countByPostId(postId);
        auditLogService.record(active ? "LIKE" : "UNLIKE", "Post", postId, null);
        return InteractionResponse.builder()
                .active(active)
                .total(total)
                .build();
    }

    @Transactional
    public InteractionResponse toggleFavorite(Long postId) {
        User user = requireCurrentUser();
        Post post = requirePost(postId);
        Favorite record = favoriteRepository.findByUserIdAndPostId(user.getId(), postId).orElse(null);
        boolean active;
        if (record != null) {
            favoriteRepository.delete(record);
            active = false;
        } else {
            favoriteRepository.save(Favorite.builder().user(user).post(post).build());
            active = true;
        }
        long total = favoriteRepository.countByPostId(postId);
        auditLogService.record(active ? "FAVORITE" : "UNFAVORITE", "Post", postId, null);
        return InteractionResponse.builder()
                .active(active)
                .total(total)
                .build();
    }

    private User requireCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    private Post requirePost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在"));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
