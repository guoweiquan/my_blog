package com.example.blog.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    OK("0", "success"),
    BAD_REQUEST("400", "请求参数异常"),
    UNAUTHORIZED("401", "未认证或认证已过期"),
    FORBIDDEN("403", "权限不足"),
    NOT_FOUND("404", "资源不存在"),
    TOO_MANY_REQUESTS("429", "请求过于频繁"),
    SERVER_ERROR("500", "服务器繁忙，请稍后再试");

    private final String code;
    private final String message;
}
