package com.example.blog.common.api;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PageResponse<T> {
    private final List<T> records;
    private final long total;
    private final long page;
    private final long size;

    public static <T> PageResponse<T> empty(long page, long size) {
        return PageResponse.<T>builder()
                .records(List.of())
                .total(0)
                .page(page)
                .size(size)
                .build();
    }
}
