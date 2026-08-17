package com.runningolle.domain.user.entity;

import com.runningolle.global.entity.BaseTimeEntity;
import com.runningolle.domain.user.enums.AccountStatus;
import com.runningolle.domain.user.enums.PreferredDifficulty;
import com.runningolle.domain.user.enums.PreferredDistance;
import com.runningolle.domain.user.enums.UserRole;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "kakao_id", nullable = false, unique = true, length = 255)
    private String kakaoId;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "text")
    private String profileImageUrl;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_distance", length = 20)
    private PreferredDistance preferredDistance;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_difficulty", length = 20)
    private PreferredDifficulty preferredDifficulty;

    @Column(name = "terms_service_agreed", nullable = false)
    @ColumnDefault("false")
    private Boolean termsServiceAgreed = false;

    @Column(name = "terms_privacy_agreed", nullable = false)
    @ColumnDefault("false")
    private Boolean termsPrivacyAgreed = false;

    @Column(name = "terms_location_agreed", nullable = false)
    @ColumnDefault("false")
    private Boolean termsLocationAgreed = false;

    @Column(name = "terms_marketing_agreed_at")
    private LocalDateTime termsMarketingAgreedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @ColumnDefault("'USER'")
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    @ColumnDefault("'ACTIVE'")
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "is_deleted", nullable = false)
    @ColumnDefault("false")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "onboarding_completed", nullable = false)
    @ColumnDefault("false")
    private Boolean onboardingCompleted = false;

    public static User createKakaoUser(String kakaoId) {
        User user = new User();
        user.kakaoId = kakaoId;
        return user;
    }

    public void completeOnboarding(
            String nickname,
            String profileImageUrl,
            String bio,
            PreferredDistance preferredDistance,
            PreferredDifficulty preferredDifficulty,
            boolean termsServiceAgreed,
            boolean termsPrivacyAgreed,
            boolean termsLocationAgreed,
            boolean termsMarketingAgreed
    ) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.preferredDistance = preferredDistance;
        this.preferredDifficulty = preferredDifficulty;
        this.termsServiceAgreed = termsServiceAgreed;
        this.termsPrivacyAgreed = termsPrivacyAgreed;
        this.termsLocationAgreed = termsLocationAgreed;
        this.termsMarketingAgreedAt = termsMarketingAgreed ? LocalDateTime.now() : null;
        this.onboardingCompleted = true;
    }

    public void withdraw() {
        this.accountStatus = AccountStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now();
    }
}
