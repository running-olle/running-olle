package com.runningolle.domain.tourism.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.runningolle.domain.tourism.enums.TourismDetailSyncStatus;
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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
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
        name = "tourism_places",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tourism_places_content_id", columnNames = "content_id")
        },
        indexes = {
                @Index(name = "idx_tourism_places_content_type_id", columnList = "content_type_id"),
                @Index(name = "idx_tourism_places_title", columnList = "title"),
                @Index(
                        name = "idx_tourism_places_detail_sync",
                        columnList = "detail_sync_status, detail_next_retry_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
public class TourismPlace extends BaseTimeEntity {

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

    @Column(name = "tel", length = 100)
    private String tel;

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

    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "first_image_url", columnDefinition = "text")
    private String firstImageUrl;

    @Column(name = "thumbnail_image_url", columnDefinition = "text")
    private String thumbnailImageUrl;

    @Column(name = "overview", columnDefinition = "text")
    private String overview;

    @Column(name = "use_time", columnDefinition = "text")
    private String useTime;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "detail_sync_status", length = 20)
    private TourismDetailSyncStatus detailSyncStatus;

    @Column(name = "detail_synced_at")
    private LocalDateTime detailSyncedAt;

    @Column(name = "detail_retry_count")
    private Integer detailRetryCount;

    @Column(name = "detail_next_retry_at")
    private LocalDateTime detailNextRetryAt;

    @Column(name = "detail_last_error", columnDefinition = "text")
    private String detailLastError;

    public static TourismPlace create(TourismPlaceSnapshot snapshot) {
        TourismPlace tourismPlace = new TourismPlace();
        tourismPlace.contentId = snapshot.contentId();
        tourismPlace.sync(snapshot);
        tourismPlace.detailSyncStatus = hasDetail(snapshot)
                ? TourismDetailSyncStatus.COMPLETE
                : TourismDetailSyncStatus.PENDING;
        tourismPlace.detailRetryCount = 0;
        return tourismPlace;
    }

    public static TourismPlace createFromInventory(TourismPlaceSnapshot snapshot) {
        TourismPlace tourismPlace = new TourismPlace();
        tourismPlace.contentId = snapshot.contentId();
        tourismPlace.syncInventory(snapshot);
        tourismPlace.detailSyncStatus = TourismDetailSyncStatus.PENDING;
        tourismPlace.detailRetryCount = 0;
        return tourismPlace;
    }

    public void sync(TourismPlaceSnapshot snapshot) {
        contentTypeId = snapshot.contentTypeId();
        title = snapshot.title();
        address = snapshot.address();
        detailAddress = snapshot.detailAddress();
        tel = snapshot.tel();
        category1 = snapshot.category1();
        category2 = snapshot.category2();
        category3 = snapshot.category3();
        areaCode = snapshot.areaCode();
        sigunguCode = snapshot.sigunguCode();
        location = snapshot.location();
        firstImageUrl = firstNonBlank(snapshot.firstImageUrl(), firstImageUrl);
        thumbnailImageUrl = firstNonBlank(snapshot.thumbnailImageUrl(), thumbnailImageUrl);
        overview = snapshot.overview();
        useTime = snapshot.useTime();
        tourCreatedTime = snapshot.tourCreatedTime();
        tourModifiedTime = snapshot.tourModifiedTime();
        rawData = snapshot.rawData();
        syncedAt = snapshot.syncedAt();
        isDeleted = false;
        deletedAt = null;
    }

    public void syncInventory(TourismPlaceSnapshot snapshot) {
        boolean modified = !Objects.equals(tourModifiedTime, snapshot.tourModifiedTime());
        boolean detailRefreshRequired = modified || (detailSyncStatus == null && !hasStoredDetail());

        contentTypeId = snapshot.contentTypeId();
        title = snapshot.title();
        address = snapshot.address();
        detailAddress = snapshot.detailAddress();
        tel = snapshot.tel();
        category1 = snapshot.category1();
        category2 = snapshot.category2();
        category3 = snapshot.category3();
        areaCode = snapshot.areaCode();
        sigunguCode = snapshot.sigunguCode();
        location = snapshot.location();
        firstImageUrl = firstNonBlank(snapshot.firstImageUrl(), firstImageUrl);
        thumbnailImageUrl = firstNonBlank(snapshot.thumbnailImageUrl(), thumbnailImageUrl);
        tourCreatedTime = snapshot.tourCreatedTime();
        tourModifiedTime = snapshot.tourModifiedTime();
        rawData = snapshot.rawData();
        syncedAt = snapshot.syncedAt();
        isDeleted = false;
        deletedAt = null;

        if (detailSyncStatus == null) {
            detailSyncStatus = hasStoredDetail()
                    ? TourismDetailSyncStatus.COMPLETE
                    : TourismDetailSyncStatus.PENDING;
        }
        if (detailRefreshRequired) {
            detailSyncStatus = TourismDetailSyncStatus.PENDING;
            detailRetryCount = 0;
            detailNextRetryAt = null;
            detailLastError = null;
        }
        if (detailRetryCount == null) {
            detailRetryCount = 0;
        }
    }

    public void completeDetailSync(TourismPlaceDetailSnapshot detail, LocalDateTime completedAt) {
        title = firstNonBlank(detail.title(), title);
        address = firstNonBlank(detail.address(), address);
        detailAddress = firstNonBlank(detail.detailAddress(), detailAddress);
        category1 = firstNonBlank(detail.category1(), category1);
        category2 = firstNonBlank(detail.category2(), category2);
        category3 = firstNonBlank(detail.category3(), category3);
        areaCode = firstNonBlank(detail.areaCode(), areaCode);
        sigunguCode = firstNonBlank(detail.sigunguCode(), sigunguCode);
        location = detail.location() == null ? location : detail.location();
        firstImageUrl = firstNonBlank(detail.firstImageUrl(), firstImageUrl);
        overview = firstNonBlank(detail.overview(), overview);
        useTime = firstNonBlank(detail.useTime(), useTime);
        rawData = detail.rawData() == null ? rawData : detail.rawData();
        detailSyncStatus = TourismDetailSyncStatus.COMPLETE;
        detailSyncedAt = completedAt;
        detailRetryCount = 0;
        detailNextRetryAt = null;
        detailLastError = null;
    }

    public void failDetailSync(String error, LocalDateTime nextRetryAt) {
        detailSyncStatus = TourismDetailSyncStatus.FAILED;
        detailRetryCount = detailRetryCount == null ? 1 : detailRetryCount + 1;
        detailNextRetryAt = nextRetryAt;
        detailLastError = error == null ? null : error.substring(0, Math.min(error.length(), 2_000));
    }

    private boolean hasStoredDetail() {
        return overview != null || useTime != null || detailSyncedAt != null;
    }

    private static boolean hasDetail(TourismPlaceSnapshot snapshot) {
        return snapshot.overview() != null || snapshot.useTime() != null;
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    public record TourismPlaceSnapshot(
            String contentId,
            String contentTypeId,
            String title,
            String address,
            String detailAddress,
            String tel,
            String category1,
            String category2,
            String category3,
            String areaCode,
            String sigunguCode,
            Point location,
            String firstImageUrl,
            String thumbnailImageUrl,
            String overview,
            String useTime,
            String tourCreatedTime,
            String tourModifiedTime,
            JsonNode rawData,
            LocalDateTime syncedAt
    ) {
    }

    public record TourismPlaceDetailSnapshot(
            String title,
            String address,
            String detailAddress,
            String category1,
            String category2,
            String category3,
            String areaCode,
            String sigunguCode,
            Point location,
            String firstImageUrl,
            String overview,
            String useTime,
            JsonNode rawData
    ) {
    }
}
