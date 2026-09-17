package com.runningolle.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum ThemeCode {
    COAST("해안", "해안 풍경"),
    FOREST("숲길", "숲길 분위기"),
    OREUM("오름", "오름 코스"),
    FOOD("맛집", "미식 동선"),
    PHOTO("포토", "포토 포인트"),
    TRADITION("전통", "제주 전통 분위기"),
    URBAN("도심", "도심 접근성");

    private final String displayName;
    private final String recommendationLabel;

    ThemeCode(String displayName, String recommendationLabel) {
        this.displayName = displayName;
        this.recommendationLabel = recommendationLabel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRecommendationLabel() {
        return recommendationLabel;
    }

    @JsonCreator
    public static ThemeCode from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ThemeCode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public static ThemeCode fromOrNull(String value) {
        try {
            return from(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @JsonValue
    public String value() {
        return name();
    }
}
