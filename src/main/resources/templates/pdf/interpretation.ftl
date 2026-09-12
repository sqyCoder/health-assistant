<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta charset="UTF-8"/>
    <title>体检报告 AI 解读</title>
    <style type="text/css">
        @page { size: A4; margin: 18mm 15mm; }
        body { font-family: ${fontFamily}; font-size: 11px; color: #303133; }
        h1 { font-size: 18px; text-align: center; margin: 0 0 4px; }
        .sub { text-align: center; color: #909399; font-size: 10px; margin-bottom: 14px; }
        .info-table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
        .info-table td { border: 1px solid #dcdfe6; padding: 4px 6px; }
        .risk { padding: 8px 10px; margin-bottom: 12px; }
        .risk-LOW { background-color: #e8f8f0; color: #1f7a4d; }
        .risk-MEDIUM { background-color: #fdf3e5; color: #b36b00; }
        .risk-HIGH { background-color: #fdeaea; color: #c0392b; }
        .section-title { font-size: 13px; font-weight: bold; margin: 14px 0 6px; border-left: 3px solid #2f7de1; padding-left: 6px; }
        .card { border: 1px solid #e4e7ed; margin-bottom: 8px; padding: 8px 10px; }
        .card-title { font-weight: bold; margin-bottom: 4px; }
        .tag { padding: 0 4px; font-size: 10px; margin-left: 6px; }
        .tag-HIGH, .tag-ABNORMAL, .tag-POSITIVE { color: #c0392b; }
        .tag-LOW { color: #b36b00; }
        .tag-NORMAL, .tag-NEGATIVE { color: #1f7a4d; }
        .tag-UNKNOWN { color: #909399; }
        .advice { color: #2f7de1; margin-top: 4px; }
        .disclaimer { margin-top: 16px; color: #909399; font-size: 9px; border-top: 1px solid #ebeef5; padding-top: 6px; }
    </style>
</head>
<body>
<h1>体检报告 AI 解读</h1>
<div class="sub">${member.name?html} · 生成时间 ${generatedAt}</div>
<table class="info-table">
    <tr>
        <td>性别</td>
        <td><#if member.gender?? && member.gender == 2>女<#else>男</#if></td>
        <td>体检日期</td>
        <td>${report.reportDate!''}</td>
    </tr>
    <tr>
        <td>体检机构</td>
        <td>${(report.institution!'')?html}</td>
        <td>异常指标</td>
        <td>${interpretation.abnormalCount!0} 项</td>
    </tr>
</table>
<div class="risk risk-${riskLevel}">
    <b>整体风险：${riskLabel}</b><br/>
    ${summary?html}
</div>
<#if focusMetrics?has_content>
    <div class="section-title">重点关注</div>
    <div>${focusMetrics?join('、')?html}</div>
</#if>
<div class="section-title">逐项解读</div>
<#list cards as card>
    <div class="card">
        <div class="card-title">${card.name?html}：${card.value?html} ${card.unit?html}<span class="tag tag-${card.status}">${card.statusLabel}</span></div>
        <div>参考范围：${card.refRange?html}</div>
        <div>${card.plainExplain?html}</div>
        <div class="advice">建议：${card.advice?html}</div>
    </div>
</#list>
<div class="disclaimer">${disclaimer?html}</div>
</body>
</html>
