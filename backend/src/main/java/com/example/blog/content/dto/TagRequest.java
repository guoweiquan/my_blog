package com.example.blog.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagRequest {

    @NotBlank
    @Size(max = 50)
    private String name;

    @Size(max = 200)
    private String description;
}
