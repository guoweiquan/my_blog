package com.example.blog.common.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ContentSanitizer {

    public String sanitize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        return Jsoup.clean(raw, Safelist.relaxed());
    }
}
