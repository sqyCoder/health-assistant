package com.health.assistant.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilsTest {

    @Test
    void shouldReturnKnownSha256Hex() {
        assertThat(HashUtils.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(HashUtils.sha256Hex((String) null)).isNull();
        assertThat(HashUtils.sha256Hex((byte[]) null)).isNull();
    }

    @Test
    void shouldBeStableForSameInput() {
        assertThat(HashUtils.sha256Hex("健康助手"))
                .isEqualTo(HashUtils.sha256Hex("健康助手"));
    }
}
