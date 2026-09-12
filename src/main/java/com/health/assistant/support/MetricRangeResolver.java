package com.health.assistant.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.health.assistant.entity.MetricReferenceRange;
import com.health.assistant.mapper.MetricReferenceRangeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MetricRangeResolver {

    private final MetricReferenceRangeMapper rangeMapper;

    public MetricReferenceRange resolve(Long metricId, Integer gender, Integer age) {
        if (metricId == null) {
            return null;
        }
        List<MetricReferenceRange> ranges = rangeMapper.selectList(Wrappers.<MetricReferenceRange>lambdaQuery()
                .eq(MetricReferenceRange::getMetricId, metricId)
                .eq(MetricReferenceRange::getStatus, 1));
        if (ranges.isEmpty()) {
            return null;
        }
        String genderCode = Integer.valueOf(2).equals(gender) ? "F" : "M";

        List<MetricReferenceRange> exactGenderAndAge = ranges.stream()
                .filter(range -> genderCode.equals(range.getGender()) && ageMatch(range, age))
                .toList();
        if (!exactGenderAndAge.isEmpty()) {
            return exactGenderAndAge.get(0);
        }
        List<MetricReferenceRange> allAndAge = ranges.stream()
                .filter(range -> "ALL".equals(range.getGender()) && ageMatch(range, age))
                .toList();
        if (!allAndAge.isEmpty()) {
            return allAndAge.get(0);
        }
        List<MetricReferenceRange> exactGender = ranges.stream()
                .filter(range -> genderCode.equals(range.getGender()))
                .toList();
        if (!exactGender.isEmpty()) {
            return exactGender.get(0);
        }
        List<MetricReferenceRange> all = ranges.stream()
                .filter(range -> "ALL".equals(range.getGender()))
                .toList();
        if (!all.isEmpty()) {
            return all.get(0);
        }
        return ranges.get(0);
    }

    private boolean ageMatch(MetricReferenceRange range, Integer age) {
        if (age == null) {
            return true;
        }
        int min = range.getAgeMin() == null ? 0 : range.getAgeMin();
        int max = range.getAgeMax() == null ? 150 : range.getAgeMax();
        return age >= min && age <= max;
    }
}
