package com.example.blog.analytics.task;

import com.example.blog.analytics.entity.ViewStat;
import com.example.blog.analytics.repository.ViewStatRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyticsScheduler {

    private static final String KEY_PV_DAILY = "pv:daily:";
    private static final String KEY_UV_DAILY = "uv:daily:";
    private static final String KEY_POST_RANKING = "post:ranking:views";

    private final StringRedisTemplate redisTemplate;
    private final ViewStatRepository viewStatRepository;

    @Scheduled(cron = "0 5 0 * * ?")
    public void archiveDailyMetrics() {
        LocalDate target = LocalDate.now().minusDays(1);
        String pvKey = KEY_PV_DAILY + target;
        String uvKey = KEY_UV_DAILY + target;
        long pv = parseLong(redisTemplate.opsForValue().get(pvKey));
        Long uvSize = redisTemplate.opsForHyperLogLog().size(uvKey);
        long uv = uvSize != null ? uvSize : 0L;
        if (pv == 0 && uv == 0) {
            cleanupKey(pvKey);
            cleanupKey(uvKey);
            return;
        }
        ViewStat stat = viewStatRepository.findByStatDateAndPostIsNull(target)
                .orElseGet(ViewStat::new);
        stat.setStatDate(target);
        stat.setPost(null);
        stat.setPageViews((int) Math.min(Integer.MAX_VALUE, pv));
        stat.setUniqueVisitors((int) Math.min(Integer.MAX_VALUE, uv));
        viewStatRepository.save(stat);
        cleanupKey(pvKey);
        cleanupKey(uvKey);
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void trimRankingBoard() {
        Long size = redisTemplate.opsForZSet().size(KEY_POST_RANKING);
        if (size == null || size <= 100) {
            return;
        }
        redisTemplate.opsForZSet().removeRange(KEY_POST_RANKING, 0, size - 101);
    }

    private void cleanupKey(String key) {
        redisTemplate.delete(key);
    }

    private long parseLong(String value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
