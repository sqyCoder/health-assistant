package com.health.assistant.support;

import com.health.assistant.entity.MetricDict;
import com.health.assistant.mapper.MetricDictMapper;
import org.junit.jupiter.api.BeforeEach;
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
class MetricMatcherTest {

    @Mock
    private MetricDictMapper metricDictMapper;

    @InjectMocks
    private MetricMatcher matcher;

    @BeforeEach
    void setUp() {
        when(metricDictMapper.selectList(any())).thenReturn(List.of(
                metric(2021L, "ALT", "谷丙转氨酶", "丙氨酸氨基转移酶,GPT"),
                metric(2022L, "AST", "谷草转氨酶", "天门冬氨酸氨基转移酶,GOT"),
                metric(2001L, "WBC", "白细胞计数", "白细胞,WBC"),
                metric(2003L, "HGB", "血红蛋白", "Hb,HGB")));
    }

    @Test
    void shouldMatchExactName() {
        MetricMatcher.MatchResult result = matcher.match("谷丙转氨酶");
        assertThat(result).isNotNull();
        assertThat(result.metric().getId()).isEqualTo(2021L);
        assertThat(result.matchType()).isEqualTo("EXACT");
    }

    @Test
    void shouldMatchAlias() {
        MetricMatcher.MatchResult result = matcher.match("GPT");
        assertThat(result).isNotNull();
        assertThat(result.metric().getId()).isEqualTo(2021L);
        assertThat(result.matchType()).isEqualTo("ALIAS");
    }

    @Test
    void shouldMatchNameWithSpacesAndFullWidth() {
        MetricMatcher.MatchResult result = matcher.match("　谷丙转氨酶 ");
        assertThat(result).isNotNull();
        assertThat(result.metric().getId()).isEqualTo(2021L);
    }

    @Test
    void shouldStripParenthesesBeforeMatching() {
        MetricMatcher.MatchResult result = matcher.match("谷丙转氨酶(ALT)");
        assertThat(result).isNotNull();
        assertThat(result.metric().getId()).isEqualTo(2021L);
        assertThat(result.matchType()).isEqualTo("EXACT");
    }

    @Test
    void shouldMatchFuzzyContainment() {
        MetricMatcher.MatchResult result = matcher.match("白细胞数");
        assertThat(result).isNotNull();
        assertThat(result.metric().getId()).isEqualTo(2001L);
        assertThat(result.matchType()).isEqualTo("FUZZY");
    }

    @Test
    void shouldReturnNullForUnknownName() {
        assertThat(matcher.match("完全不存在的神秘指标xyz")).isNull();
        assertThat(matcher.match("")).isNull();
        assertThat(matcher.match(null)).isNull();
    }

    private MetricDict metric(Long id, String code, String name, String alias) {
        MetricDict metric = new MetricDict();
        metric.setId(id);
        metric.setCode(code);
        metric.setName(name);
        metric.setNameEn(code);
        metric.setAlias(alias);
        metric.setStatus(1);
        return metric;
    }
}
