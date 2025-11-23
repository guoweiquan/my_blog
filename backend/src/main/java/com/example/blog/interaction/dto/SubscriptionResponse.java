package com.example.blog.interaction.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionResponse {

    private final boolean subscribed;
}
