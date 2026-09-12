package com.health.assistant.dto.report;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IndicatorReviewRequest {

    @NotEmpty(message = "至少填写一项指标")
    @Valid
    private List<IndicatorItemRequest> indicators;
}
