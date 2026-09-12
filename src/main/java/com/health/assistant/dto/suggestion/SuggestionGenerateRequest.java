package com.health.assistant.dto.suggestion;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SuggestionGenerateRequest {

    @NotBlank(message = "建议类型不能为空")
    private String suggestionType;

    private String sourceType;

    private Long sourceId;

    private String goal;
}
