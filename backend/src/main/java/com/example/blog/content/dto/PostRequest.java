package com.example.blog.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 200)
    private String slug;

    @Size(max = 1000)
    private String summary;

    @NotBlank
    private String content;

    private String coverUrl;

    @NotBlank
    private String status;

    private Integer readingTime;

    private String seoKeywords;

    private Set<Long> tagIds;
}
