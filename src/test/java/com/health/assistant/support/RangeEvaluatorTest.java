package com.health.assistant.support;

import com.health.assistant.common.constant.ReportConstants;
import com.health.assistant.entity.MetricReferenceRange;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RangeEvaluatorTest {

    private final RangeEvaluator evaluator = new RangeEvaluator();

    @Test
    void shouldJudgeNormalWhenWithinRange() {
        RangeEvaluator.JudgeResult result = evaluator.evaluate("42", range(9, 50, "9-50"));

        assertThat(result.status()).isEqualTo(ReportConstants.JUDGE_NORMAL);
        assertThat(result.valueNumeric()).isEqualByComparingTo("42");
    }

    @Test
    void shouldJudgeHighWhenAboveMax() {
        RangeEvaluator.JudgeResult result = evaluator.evaluate("52", range(9, 50, "9-50"));

        assertThat(result.status()).isEqualTo(ReportConstants.JUDGE_HIGH);
        assertThat(result.note()).contains("高于参考上限");
    }

    @Test
    void shouldJudgeLowWhenBelowMin() {
        RangeEvaluator.JudgeResult result = evaluator.evaluate("5", range(9, 50, "9-50"));

        assertThat(result.status()).isEqualTo(ReportConstants.JUDGE_LOW);
        assertThat(result.note()).contains("低于参考下限");
    }

    @Test
    void shouldTreatBoundaryAsNormal() {
        assertThat(evaluator.evaluate("9", range(9, 50, "9-50")).status())
                .isEqualTo(ReportConstants.JUDGE_NORMAL);
        assertThat(evaluator.evaluate("50", range(9, 50, "9-50")).status())
                .isEqualTo(ReportConstants.JUDGE_NORMAL);
    }

    @Test
    void shouldJudgeQualitativeNegativeAsNormal() {
        MetricReferenceRange range = new MetricReferenceRange();
        range.setNormalText("阴性");
        range.setRefText("阴性");

        assertThat(evaluator.evaluate("阴性", range).status())
                .isEqualTo(ReportConstants.JUDGE_NEGATIVE);
    }

    @Test
    void shouldJudgeQualitativePositive() {
        MetricReferenceRange range = new MetricReferenceRange();
        range.setNormalText("阴性");
        range.setRefText("阴性");

        assertThat(evaluator.evaluate("+", range).status())
                .isEqualTo(ReportConstants.JUDGE_POSITIVE);
        assertThat(evaluator.evaluate("阳性", range).status())
                .isEqualTo(ReportConstants.JUDGE_POSITIVE);
    }

    @Test
    void shouldReturnUnknownWhenNoRange() {
        RangeEvaluator.JudgeResult result = evaluator.evaluate("5.2", null);

        assertThat(result.status()).isEqualTo(ReportConstants.JUDGE_UNKNOWN);
        assertThat(result.valueNumeric()).isEqualByComparingTo("5.2");
    }

    @Test
    void shouldParseNumericFromVariousTexts() {
        assertThat(evaluator.parseNumeric("5.2")).isEqualByComparingTo("5.2");
        assertThat(evaluator.parseNumeric("<0.5")).isEqualByComparingTo("0.5");
        assertThat(evaluator.parseNumeric("1,234")).isEqualByComparingTo("1234");
        assertThat(evaluator.parseNumeric("阴性")).isNull();
        assertThat(evaluator.parseNumeric(null)).isNull();
    }

    private MetricReferenceRange range(double min, double max, String text) {
        MetricReferenceRange range = new MetricReferenceRange();
        range.setRefMin(BigDecimal.valueOf(min));
        range.setRefMax(BigDecimal.valueOf(max));
        range.setRefText(text);
        return range;
    }
}
