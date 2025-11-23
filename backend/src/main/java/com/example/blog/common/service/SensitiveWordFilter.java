package com.example.blog.common.service;

import com.example.blog.common.config.SecurityProperties;
import com.example.blog.common.enums.ErrorCode;
import com.example.blog.common.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SensitiveWordFilter {

    private final SecurityProperties securityProperties;

    public void assertClean(String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        List<String> words = securityProperties.getSensitiveWords();
        if (CollectionUtils.isEmpty(words)) {
            return;
        }
        String lower = text.toLowerCase();
        for (String word : words) {
            if (StringUtils.hasText(word) && lower.contains(word.toLowerCase())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "内容包含敏感词，请重新编辑");
            }
        }
    }
}
