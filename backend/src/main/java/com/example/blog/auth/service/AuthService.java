package com.example.blog.auth.service;

import com.example.blog.auth.dto.LoginRequest;
import com.example.blog.auth.dto.LoginResponse;
import com.example.blog.auth.dto.RegisterRequest;
import com.example.blog.auth.dto.UserProfileResponse;
import com.example.blog.auth.entity.Role;
import com.example.blog.auth.entity.User;
import com.example.blog.auth.repository.RoleRepository;
import com.example.blog.auth.repository.UserRepository;
import com.example.blog.auth.security.JwtTokenProvider;
import com.example.blog.auth.util.SecurityUtils;
import com.example.blog.common.enums.ErrorCode;
import com.example.blog.common.exception.BusinessException;
import com.example.blog.common.exception.ResourceNotFoundException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱已被注册");
        }
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVER_ERROR, "基础角色不存在"));
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .status(1)
                .build();
        user.addRole(userRole);
        User saved = userRepository.save(user);
        return toProfile(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));
        if (!user.isActive() || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return generateTokens(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String refreshToken) {
        Long userId = refreshTokenService.getUserId(refreshToken);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh Token 无效");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        refreshTokenService.remove(refreshToken);
        return generateTokens(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return toProfile(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.remove(refreshToken);
        }
    }

    private LoginResponse generateTokens(User user) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = UUID.randomUUID().toString().replaceAll("-", "");
        refreshTokenService.store(user.getId(), refreshToken);
        return new LoginResponse(accessToken, refreshToken);
    }

    private UserProfileResponse toProfile(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roles(roles)
                .build();
    }
}
