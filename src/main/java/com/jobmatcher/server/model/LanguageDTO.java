package com.jobmatcher.server.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LanguageDTO {

    private Integer id;

    @NotBlank
    @Size(max = 50)
    private String name;
}
