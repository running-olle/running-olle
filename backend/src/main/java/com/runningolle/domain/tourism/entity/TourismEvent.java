package com.runningolle.domain.tourism.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.runningolle.global.entity.BaseTimeEntity;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
        name = "tourism_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tourism_events_content_id", columnNames = "content_id")
        },
        indexes = {
                @Index(name = "idx_tourism_events_date", columnList = "event_start_date,event_end_date"),
                @Index(name = "idx_tourism_events_running", columnList = "is_running_related"),
                @Index(name = "idx_tourism_events_title", columnList = "title")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
public class TourismEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "content_id", nullable = false, length = 100)
    private String contentId;

    @Column(name = "content_type_id", nullable = false, length = 10)
    private String contentTypeId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "detail_address", length = 255)
    private String detailAddress;

    @Column(name = "venue_name", length = 200)
    private String venueName;

    @Column(name = "organizer", length = 200)
    private String organizer;

    @Column(name = "tel", length = 100)
    private String tel;

    @Column(name = "homepage", columnDefinition = "text")
    private String homepage;

    @Column(name = "event_start_date", nullable = false)
    private LocalDate eventStartDate;

    @Column(name = "event_end_date", nullable = false)
    private LocalDate eventEndDate;

    @Column(name = "category1", length = 30)
    private String category1;

    @Column(name = "category2", length = 30)
    private String category2;

    @Column(name = "category3", length = 30)
    private String category3;

    @Column(name = "area_code", length = 10)
    private String areaCode;

    @Column(name = "sigungu_code", length = 10)
    private String sigunguCode;

    @Column(name = "location", columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "first_image_url", columnDefinition = "text")
    private String firstImageUrl;

    @Column(name = "thumbnail_image_url", columnDefinition = "text")
    private String thumbnailImageUrl;

    @Column(name = "overview", columnDefinition = "text")
    private String overview;

    @Column(name = "is_running_related", nullable = false)
    @ColumnDefault("false")
    private Boolean runningRelated = false;

    @Column(name = "running_score", nullable = false)
    @ColumnDefault("0")
    private Integer runningScore = 0;

    @Column(name = "tour_created_time", length = 20)
    private String tourCreatedTime;

    @Column(name = "tour_modified_time", length = 20)
    private String tourModifiedTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private JsonNode rawData;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Column(name = "is_deleted", nullable = false)
    @ColumnDefault("false")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static TourismEvent create(TourismEventSnapshot snapshot) {
        TourismEvent event = new TourismEvent();
        event.contentId = snapshot.contentId();
        event.sync(snapshot);
        return event;
    }

    public void sync(TourismEventSnapshot snapshot) {
        contentTypeId = snapshot.contentTypeId();
        title = snapshot.title();
        address = snapshot.address();
        detailAddress = snapshot.detailAddress();
        venueName = snapshot.venueName();
        organizer = snapshot.organizer();
        tel = snapshot.tel();
        homepage = snapshot.homepage();
        eventStartDate = snapshot.eventStartDate();
        eventEndDate = snapshot.eventEndDate();
        category1 = snapshot.category1();
        category2 = snapshot.category2();
        category3 = snapshot.category3();
        areaCode = snapshot.areaCode();
        sigunguCode = snapshot.sigunguCode();
        location = snapshot.location();
        firstImageUrl = snapshot.firstImageUrl();
        thumbnailImageUrl = snapshot.thumbnailImageUrl();
        overview = snapshot.overview();
        runningRelated = snapshot.runningRelated();
        runningScore = snapshot.runningScore();
        tourCreatedTime = snapshot.tourCreatedTime();
        tourModifiedTime = snapshot.tourModifiedTime();
        rawData = snapshot.rawData();
        syncedAt = snapshot.syncedAt();
        isDeleted = false;
        deletedAt = null;
    }

    public record TourismEventSnapshot(
            String contentId,
            String contentTypeId,
            String title,
            String address,
            String detailAddress,
            String venueName,
            String organizer,
            String tel,
            String homepage,
            LocalDate eventStartDate,
            LocalDate eventEndDate,
            String category1,
            String category2,
            String category3,
            String areaCode,
            String sigunguCode,
            Point location,
            String firstImageUrl,
            String thumbnailImageUrl,
            String overview,
            boolean runningRelated,
            int runningScore,
            String tourCreatedTime,
            String tourModifiedTime,
            JsonNode rawData,
            LocalDateTime syncedAt
    ) {
    }
}
