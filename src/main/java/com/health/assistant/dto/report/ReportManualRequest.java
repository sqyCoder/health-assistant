package com.health.assistant.dto.report;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ReportManualRequest {

    @NotNull(message = "成员不能为空")
    private Long memberId;

    @Size(max = 100, message = "报告名称最长 100 个字符")
    private String reportName;

    @NotNull(message = "体检日期不能为空")
    private LocalDate reportDate;

    @Size(max = 100, message = "体检机构最长 100 个字符")
    private String institution;

    @NotBlank(message = "报告类型不能为空")
    private String reportType;

    @Size(max = 500, message = "备注最长 500 个字符")
    private String remark;

    @NotEmpty(message = "至少填写一项指标")
    @Valid
    private List<IndicatorItemRequest> indicators;
}
