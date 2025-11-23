package com.example.blog.interaction.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InteractionResponse {

    private final boolean active;
    private final long total;
}
