package com.health.assistant.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiJsonUtilsTest {

    @Test
    void shouldStripJsonCodeFence() {
        String content = "```json\n{\"indicators\":[]}\n```";
        assertThat(AiJsonUtils.stripCodeFences(content)).isEqualTo("{\"indicators\":[]}");
    }

    @Test
    void shouldStripPlainCodeFence() {
        String content = "```\n{\"a\":1}\n```";
        assertThat(AiJsonUtils.stripCodeFences(content)).isEqualTo("{\"a\":1}");
    }

    @Test
    void shouldKeepPlainJsonUntouched() {
        assertThat(AiJsonUtils.stripCodeFences(" {\"a\":1} ")).isEqualTo("{\"a\":1}");
    }

    @Test
    void shouldHandleNull() {
        assertThat(AiJsonUtils.stripCodeFences(null)).isEmpty();
    }
}
