package com.health.assistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.health.assistant.common.api.PageResult;
import com.health.assistant.common.exception.BizException;
import com.health.assistant.common.exception.ErrorCode;
import com.health.assistant.dto.admin.MetricRangeItem;
import com.health.assistant.dto.admin.MetricUpsertRequest;
import com.health.assistant.entity.MetricCategory;
import com.health.assistant.entity.MetricDict;
import com.health.assistant.entity.MetricReferenceRange;
import com.health.assistant.mapper.MetricCategoryMapper;
import com.health.assistant.mapper.MetricDictMapper;
import com.health.assistant.mapper.MetricReferenceRangeMapper;
import com.health.assistant.service.MetricService;
import com.health.assistant.vo.MetricCategoryVO;
import com.health.assistant.vo.MetricDetailVO;
import com.health.assistant.vo.MetricRangeVO;
import com.health.assistant.vo.MetricVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetricServiceImpl implements MetricService {

    private final MetricCategoryMapper categoryMapper;
    private final MetricDictMapper metricDictMapper;
    private final MetricReferenceRangeMapper rangeMapper;

    @Override
    public List<MetricCategoryVO> listCategories() {
        return categoryMapper.selectList(Wrappers.<MetricCategory>lambdaQuery()
                        .eq(MetricCategory::getStatus, 1)
                        .orderByAsc(MetricCategory::getSort))
                .stream()
                .map(category -> MetricCategoryVO.builder()
                        .id(category.getId())
                        .code(category.getCode())
                        .name(category.getName())
                        .sort(category.getSort())
                        .build())
                .toList();
    }

    @Override
    public PageResult<MetricVO> search(String keyword, Long categoryId, long pageNum, long pageSize) {
        LambdaQueryWrapper<MetricDict> wrapper = Wrappers.<MetricDict>lambdaQuery()
                .eq(MetricDict::getStatus, 1)
                .eq(categoryId != null, MetricDict::getCategoryId, categoryId)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(MetricDict::getName, keyword)
                        .or().like(MetricDict::getNameEn, keyword)
                        .or().like(MetricDict::getAlias, keyword)
                        .or().like(MetricDict::getCode, keyword))
                .orderByAsc(MetricDict::getCategoryId)
                .orderByAsc(MetricDict::getSort);
        Page<MetricDict> page = metricDictMapper.selectPage(Page.of(pageNum, pageSize), wrapper);

        Map<Long, String> categoryNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(MetricCategory::getId, MetricCategory::getName, (a, b) -> a));
        return PageResult.of(page, metric -> toVO(metric, categoryNames));
    }

    @Override
    public MetricDetailVO getDetail(Long id) {
        MetricDict metric = metricDictMapper.selectById(id);
        if (metric == null || !Integer.valueOf(1).equals(metric.getStatus())) {
            throw new BizException(ErrorCode.NOT_FOUND, "指标不存在");
        }
        return toDetail(metric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetricDetailVO adminCreate(MetricUpsertRequest request) {
        Long exists = metricDictMapper.selectCount(Wrappers.<MetricDict>lambdaQuery()
                .eq(MetricDict::getCode, request.getCode()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "指标编码已存在: " + request.getCode());
        }
        MetricDict metric = new MetricDict();
        applyMetric(metric, request);
        metricDictMapper.insert(metric);
        return toDetail(metric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetricDetailVO adminUpdate(Long id, MetricUpsertRequest request) {
        MetricDict metric = metricDictMapper.selectById(id);
        if (metric == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "指标不存在");
        }
        applyMetric(metric, request);
        metricDictMapper.updateById(metric);
        return toDetail(metric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "状态取值非法");
        }
        MetricDict metric = metricDictMapper.selectById(id);
        if (metric == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "指标不存在");
        }
        metric.setStatus(status);
        metricDictMapper.updateById(metric);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetricDetailVO adminReplaceRanges(Long id, List<MetricRangeItem> ranges) {
        MetricDict metric = metricDictMapper.selectById(id);
        if (metric == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "指标不存在");
        }
        rangeMapper.delete(Wrappers.<MetricReferenceRange>lambdaQuery()
                .eq(MetricReferenceRange::getMetricId, id));
        if (ranges != null) {
            for (MetricRangeItem item : ranges) {
                MetricReferenceRange range = new MetricReferenceRange();
                range.setMetricId(id);
                range.setGender(StringUtils.hasText(item.getGender()) ? item.getGender() : "ALL");
                range.setAgeMin(item.getAgeMin() == null ? 0 : item.getAgeMin());
                range.setAgeMax(item.getAgeMax() == null ? 150 : item.getAgeMax());
                range.setRefMin(item.getRefMin());
                range.setRefMax(item.getRefMax());
                range.setRefText(item.getRefText());
                range.setNormalText(item.getNormalText());
                range.setStatus(1);
                rangeMapper.insert(range);
            }
        }
        return toDetail(metric);
    }

    private void applyMetric(MetricDict metric, MetricUpsertRequest request) {
        metric.setCategoryId(request.getCategoryId());
        metric.setCode(request.getCode());
        metric.setName(request.getName());
        metric.setNameEn(request.getNameEn());
        metric.setAlias(request.getAlias());
        metric.setValueType(StringUtils.hasText(request.getValueType()) ? request.getValueType() : "NUMERIC");
        metric.setUnit(request.getUnit());
        metric.setDescription(request.getDescription());
        metric.setHighDesc(request.getHighDesc());
        metric.setLowDesc(request.getLowDesc());
        metric.setSort(request.getSort() == null ? 0 : request.getSort());
        metric.setStatus(request.getStatus() == null ? 1 : request.getStatus());
    }

    private MetricDetailVO toDetail(MetricDict metric) {
        MetricCategory category = categoryMapper.selectById(metric.getCategoryId());
        List<MetricRangeVO> ranges = rangeMapper.selectList(Wrappers.<MetricReferenceRange>lambdaQuery()
                        .eq(MetricReferenceRange::getMetricId, metric.getId())
                        .eq(MetricReferenceRange::getStatus, 1)
                        .orderByAsc(MetricReferenceRange::getGender))
                .stream()
                .map(MetricRangeVO::from)
                .toList();
        return MetricDetailVO.builder()
                .id(metric.getId())
                .code(metric.getCode())
                .name(metric.getName())
                .nameEn(metric.getNameEn())
                .alias(metric.getAlias())
                .valueType(metric.getValueType())
                .unit(metric.getUnit())
                .categoryName(category == null ? null : category.getName())
                .description(metric.getDescription())
                .highDesc(metric.getHighDesc())
                .lowDesc(metric.getLowDesc())
                .ranges(ranges)
                .build();
    }

    private MetricVO toVO(MetricDict metric, Map<Long, String> categoryNames) {
        return MetricVO.builder()
                .id(metric.getId())
                .code(metric.getCode())
                .name(metric.getName())
                .nameEn(metric.getNameEn())
                .alias(metric.getAlias())
                .valueType(metric.getValueType())
                .unit(metric.getUnit())
                .categoryName(categoryNames.get(metric.getCategoryId()))
                .description(metric.getDescription())
                .build();
    }
}
