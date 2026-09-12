package com.health.assistant.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportTextCleanerTest {

    private final ReportTextCleaner cleaner = new ReportTextCleaner();

    @Test
    void shouldRemovePageNumbersAndRepeatedLines() {
        String raw = """
                某某医院体检报告
                第 1 页
                谷丙转氨酶 52 U/L 9-50
                某某医院体检报告
                空腹血糖 5.4 mmol/L
                某某医院体检报告
                第 2 页
                白细胞计数 6.1
                """;

        String cleaned = cleaner.clean(raw);

        assertThat(cleaned).doesNotContain("第 1 页").doesNotContain("第 2 页");
        assertThat(cleaned).doesNotContain("某某医院体检报告");
        assertThat(cleaned).contains("谷丙转氨酶").contains("空腹血糖").contains("白细胞计数");
    }

    @Test
    void shouldNormalizeFullWidthCharacters() {
        String cleaned = cleaner.clean("血糖：５.４ｍｍｏｌ／Ｌ（空腹）");

        assertThat(cleaned).contains("5.4").contains("(空腹)");
    }

    @Test
    void shouldReturnEmptyForBlankInput() {
        assertThat(cleaner.clean(null)).isEmpty();
        assertThat(cleaner.clean("   ")).isEmpty();
    }
}
