package com.runningolle.domain.trip.entity;

import com.runningolle.global.entity.BaseTimeEntity;
import com.runningolle.domain.user.entity.User;
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
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "trips")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
public class Trip extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "thumbnail_image_url", columnDefinition = "text")
    private String thumbnailImageUrl;

    public static Trip create(User user, String name, String region, LocalDate startDate, LocalDate endDate, String thumbnailImageUrl) {
        Trip trip = new Trip();
        trip.user = user;
        trip.name = name;
        trip.region = region;
        trip.startDate = startDate;
        trip.endDate = endDate;
        trip.thumbnailImageUrl = thumbnailImageUrl;
        return trip;
    }

    public void update(String name, String region, LocalDate startDate, LocalDate endDate, String thumbnailImageUrl) {
        this.name = name;
        this.region = region;
        this.startDate = startDate;
        this.endDate = endDate;
        this.thumbnailImageUrl = thumbnailImageUrl;
    }
}
