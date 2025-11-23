package com.example.blog.content.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final Integer postCount;
    private final boolean subscribed;
}
