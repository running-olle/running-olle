package com.runningolle.domain.user.enums;

public enum UserTypeCode {
    ACTIVE_RUNNER("활동적인 러너"),
    RELAXED_TRAVELER("여유로운 여행자"),
    JEJU_RESIDENT("제주 거주민");

    private final String displayName;

    UserTypeCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
