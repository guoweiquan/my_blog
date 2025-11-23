package com.example.blog.common.service;

import com.example.blog.auth.entity.User;
import com.example.blog.auth.repository.UserRepository;
import com.example.blog.auth.util.SecurityUtils;
import com.example.blog.common.entity.AuditLog;
import com.example.blog.common.repository.AuditLogRepository;
import com.example.blog.common.util.RequestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public void record(String action, String entityType, Long entityId, Object changes) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        String changeString = serializeChanges(changes);
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .changes(changeString)
                .ipAddress(RequestUtils.getClientIp())
                .userAgent(RequestUtils.getUserAgent())
                .build();
        auditLogRepository.save(log);
    }

    private String serializeChanges(Object changes) {
        if (changes == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (JsonProcessingException e) {
            return Optional.ofNullable(changes.toString()).orElse(null);
        }
    }
}
