package com.health.assistant.support;

import com.health.assistant.entity.MetricReferenceRange;
import com.health.assistant.mapper.MetricReferenceRangeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricRangeResolverTest {

    @Mock
    private MetricReferenceRangeMapper rangeMapper;

    @InjectMocks
    private MetricRangeResolver resolver;

    @Test
    void shouldPreferExactGenderAndAge() {
        when(rangeMapper.selectList(any())).thenReturn(List.of(
                range("M", 18, 150, 9, 50),
                range("F", 18, 150, 7, 40),
                range("ALL", 0, 17, 0, 40)));

        MetricReferenceRange result = resolver.resolve(2021L, 1, 59);

        assertThat(result.getGender()).isEqualTo("M");
        assertThat(result.getRefMin()).isEqualByComparingTo("9");
    }

    @Test
    void shouldSelectFemaleRange() {
        when(rangeMapper.selectList(any())).thenReturn(List.of(
                range("M", 18, 150, 9, 50),
                range("F", 18, 150, 7, 40),
                range("ALL", 0, 17, 0, 40)));

        MetricReferenceRange result = resolver.resolve(2021L, 2, 35);

        assertThat(result.getGender()).isEqualTo("F");
        assertThat(result.getRefMax()).isEqualByComparingTo("40");
    }

    @Test
    void shouldSelectChildRangeByAge() {
        when(rangeMapper.selectList(any())).thenReturn(List.of(
                range("M", 18, 150, 9, 50),
                range("F", 18, 150, 7, 40),
                range("ALL", 0, 17, 0, 40)));

        MetricReferenceRange result = resolver.resolve(2021L, 1, 10);

        assertThat(result.getGender()).isEqualTo("ALL");
        assertThat(result.getRefMin()).isEqualByComparingTo("0");
        assertThat(result.getRefMax()).isEqualByComparingTo("40");
    }

    @Test
    void shouldReturnNullWhenMetricIdMissing() {
        assertThat(resolver.resolve(null, 1, 30)).isNull();
    }

    private MetricReferenceRange range(String gender, int ageMin, int ageMax, double min, double max) {
        MetricReferenceRange range = new MetricReferenceRange();
        range.setGender(gender);
        range.setAgeMin(ageMin);
        range.setAgeMax(ageMax);
        range.setRefMin(java.math.BigDecimal.valueOf(min));
        range.setRefMax(java.math.BigDecimal.valueOf(max));
        return range;
    }
}
