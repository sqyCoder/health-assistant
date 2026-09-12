package com.health.assistant.service;

import com.health.assistant.common.api.PageResult;
import com.health.assistant.dto.admin.MetricRangeItem;
import com.health.assistant.dto.admin.MetricUpsertRequest;
import com.health.assistant.vo.MetricCategoryVO;
import com.health.assistant.vo.MetricDetailVO;
import com.health.assistant.vo.MetricVO;

import java.util.List;

public interface MetricService {

    List<MetricCategoryVO> listCategories();

    PageResult<MetricVO> search(String keyword, Long categoryId, long pageNum, long pageSize);

    MetricDetailVO getDetail(Long id);

    MetricDetailVO adminCreate(MetricUpsertRequest request);

    MetricDetailVO adminUpdate(Long id, MetricUpsertRequest request);

    void adminUpdateStatus(Long id, Integer status);

    MetricDetailVO adminReplaceRanges(Long id, List<MetricRangeItem> ranges);
}
