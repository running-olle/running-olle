package com.runningolle.domain.course.entity;

import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.global.entity.BaseTimeEntity;
import com.runningolle.domain.user.entity.User;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "course_type", nullable = false, length = 20)
    private CourseType courseType;

    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @Column(name = "elevation_gain_m", precision = 10, scale = 2)
    private BigDecimal elevationGainM;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "surface_asphalt_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal surfaceAsphaltPct;

    @Column(name = "surface_dirt_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal surfaceDirtPct;

    @Column(name = "surface_stairs_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal surfaceStairsPct;

    @Column(name = "route", nullable = false, columnDefinition = "geometry(LineString,4326)")
    private LineString route;

    @Column(name = "start_point", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point startPoint;

    @Column(name = "gpx_file_url", columnDefinition = "text")
    private String gpxFileUrl;

    @Column(name = "thumbnail_image_url", columnDefinition = "text")
    private String thumbnailImageUrl;

    @Column(name = "is_public", nullable = false)
    @ColumnDefault("true")
    private Boolean isPublic = true;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    @ColumnDefault("0")
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "completion_count", nullable = false)
    @ColumnDefault("0")
    private Integer completionCount = 0;

    @Column(name = "is_deleted", nullable = false)
    @ColumnDefault("false")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void updateRatingAvg(BigDecimal ratingAvg) {
        BigDecimal normalized = ratingAvg == null ? BigDecimal.ZERO : ratingAvg;
        this.ratingAvg = normalized.setScale(2, RoundingMode.HALF_UP);
    }
}
