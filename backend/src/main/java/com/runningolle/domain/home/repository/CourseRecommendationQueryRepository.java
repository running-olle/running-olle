package com.runningolle.domain.home.repository;

import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CourseRecommendationQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<CourseRecommendationCandidate> findRecommendationCandidates(Double latitude, Double longitude) {
        boolean hasLocation = latitude != null && longitude != null;
        String sql = hasLocation
                ? """
                select
                    c.id,
                    c.name,
                    c.description,
                    c.course_type,
                    c.distance_km,
                    c.difficulty,
                    c.elevation_gain_m,
                    c.rating_avg,
                    c.completion_count,
                    ST_Distance(
                        c.start_point::geography,
                        ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography
                    ) as distance_meters
                from courses c
                where c.is_public = true
                  and c.is_deleted = false
                order by c.created_at desc
                """
                : """
                select
                    c.id,
                    c.name,
                    c.description,
                    c.course_type,
                    c.distance_km,
                    c.difficulty,
                    c.elevation_gain_m,
                    c.rating_avg,
                    c.completion_count,
                    null::double precision as distance_meters
                from courses c
                where c.is_public = true
                  and c.is_deleted = false
                order by c.created_at desc
                """;

        var query = entityManager.createNativeQuery(sql);
        if (hasLocation) {
            query.setParameter("latitude", latitude);
            query.setParameter("longitude", longitude);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<CourseRecommendationCandidate> candidates = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            candidates.add(new CourseRecommendationCandidate(
                    toUuid(row[0]),
                    String.valueOf(row[1]),
                    toNullableString(row[2]),
                    CourseType.valueOf(String.valueOf(row[3])),
                    toBigDecimal(row[4]),
                    Difficulty.valueOf(String.valueOf(row[5])),
                    toBigDecimal(row[6]),
                    toNullableBigDecimal(row[7]),
                    toInteger(row[8]),
                    toDouble(row[9])
            ));
        }
        return candidates;
    }

    private UUID toUuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(String.valueOf(value));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }

    private BigDecimal toNullableBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }

    private Integer toInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double doubleValue) {
            return doubleValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private String toNullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public record CourseRecommendationCandidate(
            UUID courseId,
            String courseName,
            String description,
            CourseType courseType,
            BigDecimal distanceKm,
            Difficulty difficulty,
            BigDecimal elevationGainM,
            BigDecimal averageRating,
            Integer completionCount,
            Double distanceMeters
    ) {
    }
}
