package com.example.blog.common.service;

import com.example.blog.common.enums.ErrorCode;
import com.example.blog.common.exception.BusinessException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public void assertAllowed(String key, long maxCount, Duration window, String message) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        if (count != null && count > maxCount) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS,
                    StringUtils.hasText(message) ? message : "请求过于频繁，请稍后再试");
        }
    }
}
