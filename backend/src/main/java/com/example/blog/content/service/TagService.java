package com.example.blog.content.service;

import com.example.blog.common.enums.ErrorCode;
import com.example.blog.common.exception.BusinessException;
import com.example.blog.common.exception.ResourceNotFoundException;
import com.example.blog.common.service.AuditLogService;
import com.example.blog.content.dto.TagRequest;
import com.example.blog.content.dto.TagResponse;
import com.example.blog.content.entity.Tag;
import com.example.blog.content.repository.TagRepository;
import com.example.blog.interaction.entity.Subscription;
import com.example.blog.interaction.repository.SubscriptionRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "tags:list", key = "#userId != null ? 'user:' + #userId : 'guest'")
    public List<TagResponse> findAll(Long userId) {
        Set<Long> subscribed = Collections.emptySet();
        if (userId != null) {
            subscribed = subscriptionRepository.findByUserId(userId).stream()
                    .map(Subscription::getTag)
                    .filter(tag -> tag != null && tag.getId() != null)
                    .map(Tag::getId)
                    .collect(Collectors.toSet());
        }
        Set<Long> finalSubscribed = subscribed;
        return tagRepository.findAll().stream()
                .map(tag -> toResponse(tag, finalSubscribed.contains(tag.getId())))
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "tags:list", allEntries = true)
    public TagResponse create(TagRequest request) {
        tagRepository.findByName(request.getName()).ifPresent(tag -> {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签名已存在");
        });
        Tag tag = Tag.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Tag saved = tagRepository.save(tag);
        auditLogService.record("CREATE_TAG", "Tag", saved.getId(), Map.of("name", saved.getName()));
        return toResponse(saved, false);
    }

    @Transactional
    @CacheEvict(cacheNames = "tags:list", allEntries = true)
    public TagResponse update(Long id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("标签不存在"));
        tagRepository.findByName(request.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "标签名已存在");
                });
        tag.setName(request.getName());
        tag.setDescription(request.getDescription());
        Tag saved = tagRepository.save(tag);
        auditLogService.record("UPDATE_TAG", "Tag", saved.getId(), Map.of("name", saved.getName()));
        return toResponse(saved, false);
    }

    @Transactional
    @CacheEvict(cacheNames = "tags:list", allEntries = true)
    public void delete(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("标签不存在"));
        tagRepository.delete(tag);
        auditLogService.record("DELETE_TAG", "Tag", id, Map.of("name", tag.getName()));
    }

    private TagResponse toResponse(Tag tag, boolean subscribed) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .description(tag.getDescription())
                .postCount(tag.getPostCount())
                .subscribed(subscribed)
                .build();
    }
}
