package com.runningolle.domain.user.entity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "user_notification_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
public class UserNotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "marketing_agreed", nullable = false)
    @ColumnDefault("false")
    private Boolean marketingAgreed = false;

    @Column(name = "recommended_course", nullable = false)
    @ColumnDefault("true")
    private Boolean recommendedCourse = true;

    @Column(name = "weather", nullable = false)
    @ColumnDefault("true")
    private Boolean weather = true;

    @Column(name = "saved_course_update", nullable = false)
    @ColumnDefault("true")
    private Boolean savedCourseUpdate = true;

    @Column(name = "meetup_invite", nullable = false)
    @ColumnDefault("true")
    private Boolean meetupInvite = true;

    @Column(name = "comment_like", nullable = false)
    @ColumnDefault("true")
    private Boolean commentLike = true;

    @Column(name = "tier_change", nullable = false)
    @ColumnDefault("true")
    private Boolean tierChange = true;

    @Column(name = "event_challenge", nullable = false)
    @ColumnDefault("true")
    private Boolean eventChallenge = true;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static UserNotificationSetting create(
            User user,
            boolean marketingAgreed,
            boolean recommendedCourse,
            boolean weather,
            boolean meetupInvite,
            boolean commentLike
    ) {
        UserNotificationSetting setting = new UserNotificationSetting();
        setting.user = user;
        setting.marketingAgreed = marketingAgreed;
        setting.recommendedCourse = recommendedCourse;
        setting.weather = weather;
        setting.meetupInvite = meetupInvite;
        setting.commentLike = commentLike;
        return setting;
    }
}
