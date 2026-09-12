<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta charset="UTF-8"/>
    <title>个性化锻炼与康复建议</title>
    <style type="text/css">
        @page { size: A4; margin: 18mm 15mm; }
        body { font-family: ${fontFamily}; font-size: 11px; color: #303133; }
        h1 { font-size: 18px; text-align: center; margin: 0 0 4px; }
        .sub { text-align: center; color: #909399; font-size: 10px; margin-bottom: 14px; }
        .info-table { width: 100%; border-collapse: collapse; margin-bottom: 12px; }
        .info-table td { border: 1px solid #dcdfe6; padding: 4px 6px; }
        .section-title { font-size: 13px; font-weight: bold; margin: 14px 0 6px; border-left: 3px solid #2b7de1; padding-left: 6px; }
        .plan-table { width: 100%; border-collapse: collapse; }
        .plan-table th, .plan-table td { border: 1px solid #dcdfe6; padding: 4px 6px; font-size: 10px; text-align: left; }
        .plan-table th { background-color: #eef4fd; }
        .rehab { border: 1px solid #e4e7ed; margin-bottom: 6px; padding: 6px 8px; }
        .caution { color: #c0392b; font-size: 10px; }
        .precautions { border: 1px solid #f5a524; background-color: #fdf3e5; padding: 8px 10px; white-space: pre-wrap; }
        .disclaimer { margin-top: 16px; color: #909399; font-size: 9px; border-top: 1px solid #ebeef5; padding-top: 6px; }
    </style>
</head>
<body>
<h1>个性化锻炼与康复建议</h1>
<div class="sub">${member.name?html} · 目标：${goalLabel} · 生成时间 ${generatedAt}</div>
<table class="info-table">
    <tr>
        <td>性别</td>
        <td><#if member.gender?? && member.gender == 2>女<#else>男</#if></td>
        <td>身高</td>
        <td>${(member.heightCm!'')?html} cm</td>
        <td>体重</td>
        <td>${(member.weightKg!'')?html} kg</td>
    </tr>
</table>
<div class="section-title">现状评估</div>
<div>${assessment?html}</div>
<div class="section-title">周训练计划</div>
<table class="plan-table">
    <tr>
        <th style="width: 9%">星期</th>
        <th style="width: 16%">项目</th>
        <th style="width: 10%">时长</th>
        <th style="width: 10%">强度</th>
        <th>内容</th>
        <th style="width: 20%">注意</th>
    </tr>
    <#list weeklyPlan as day>
        <tr>
            <td>${day.day}</td>
            <td>${day.title?html}</td>
            <td>${day.durationMin} 分钟</td>
            <td>${day.intensity}</td>
            <td>${day.content?html}</td>
            <td>${day.notes?html}</td>
        </tr>
    </#list>
</table>
<#if rehabList?has_content>
    <div class="section-title">康复建议</div>
    <#list rehabList as rehab>
        <div class="rehab">
            <b>${rehab.target?html}</b>：${rehab.action?html}（${rehab.frequency?html}）
            <#if rehab.caution?has_content><div class="caution">注意：${rehab.caution?html}</div></#if>
        </div>
    </#list>
</#if>
<#if precautions?has_content>
    <div class="section-title">禁忌与注意事项</div>
    <div class="precautions">${precautions?html}</div>
</#if>
<#if progressMetrics?has_content>
    <div class="section-title">建议跟踪的指标</div>
    <div>${progressMetrics?join('、')?html}</div>
</#if>
<div class="disclaimer">${disclaimer?html}</div>
</body>
</html>
