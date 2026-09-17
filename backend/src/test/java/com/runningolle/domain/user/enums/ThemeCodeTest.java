package com.runningolle.domain.user.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ThemeCodeTest {

    @Test
    void normalizesLegacyLowercaseCodes() {
        assertThat(ThemeCode.from(" coast ")).isEqualTo(ThemeCode.COAST);
    }

    @Test
    void safelyRejectsUnknownStoredValue() {
        assertThat(ThemeCode.fromOrNull("legacy-custom-theme")).isNull();
    }
}
