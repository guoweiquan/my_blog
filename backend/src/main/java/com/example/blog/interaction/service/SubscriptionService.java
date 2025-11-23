package com.example.blog.interaction.service;

import com.example.blog.auth.entity.User;
import com.example.blog.auth.repository.UserRepository;
import com.example.blog.auth.util.SecurityUtils;
import com.example.blog.common.enums.ErrorCode;
import com.example.blog.common.exception.BusinessException;
import com.example.blog.common.exception.ResourceNotFoundException;
import com.example.blog.common.service.AuditLogService;
import com.example.blog.content.entity.Tag;
import com.example.blog.content.repository.TagRepository;
import com.example.blog.interaction.dto.SubscriptionResponse;
import com.example.blog.interaction.entity.Subscription;
import com.example.blog.interaction.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SubscriptionResponse toggle(Long tagId) {
        User user = requireCurrentUser();
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("标签不存在"));
        Subscription subscription = subscriptionRepository.findByUserIdAndTagId(user.getId(), tagId).orElse(null);
        boolean subscribed;
        if (subscription != null) {
            subscriptionRepository.delete(subscription);
            subscribed = false;
        } else {
            subscriptionRepository.save(Subscription.builder().user(user).tag(tag).build());
            subscribed = true;
        }
        auditLogService.record(subscribed ? "SUBSCRIBE" : "UNSUBSCRIBE", "Tag", tagId, null);
        return SubscriptionResponse.builder().subscribed(subscribed).build();
    }

    private User requireCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }
}
