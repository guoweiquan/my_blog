package com.example.blog.content.service;

import com.example.blog.common.enums.ErrorCode;
import com.example.blog.common.exception.BusinessException;
import com.example.blog.common.exception.ResourceNotFoundException;
import com.example.blog.content.dto.TagRequest;
import com.example.blog.content.dto.TagResponse;
import com.example.blog.content.entity.Tag;
import com.example.blog.content.repository.TagRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<TagResponse> findAll() {
        return tagRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        tagRepository.findByName(request.getName()).ifPresent(tag -> {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标签名已存在");
        });
        Tag tag = Tag.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
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
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public void delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("标签不存在");
        }
        tagRepository.deleteById(id);
    }

    private TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .description(tag.getDescription())
                .postCount(tag.getPostCount())
                .build();
    }
}
