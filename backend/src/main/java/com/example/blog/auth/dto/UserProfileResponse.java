package com.example.blog.auth.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponse {

    private final Long id;
    private final String username;
    private final String nickname;
    private final String email;
    private final String avatarUrl;
    private final Set<String> roles;
}
