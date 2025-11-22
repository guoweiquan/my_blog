package com.example.blog.auth.service;

import com.example.blog.auth.security.JwtProperties;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public void store(Long userId, String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        redisTemplate.opsForValue().set(buildKey(token), String.valueOf(userId),
                jwtProperties.getRefreshTokenExpiration(), TimeUnit.MILLISECONDS);
    }

    public Long getUserId(String token) {
        String value = redisTemplate.opsForValue().get(buildKey(token));
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return Long.valueOf(value);
    }

    public void remove(String token) {
        redisTemplate.delete(buildKey(token));
    }

    private String buildKey(String token) {
        return KEY_PREFIX + token;
    }
}
