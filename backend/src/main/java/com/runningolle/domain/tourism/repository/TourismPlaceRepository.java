package com.runningolle.domain.tourism.repository;

import com.runningolle.domain.tourism.entity.TourismPlace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TourismPlaceRepository extends JpaRepository<TourismPlace, UUID> {

    Optional<TourismPlace> findByContentId(String contentId);

    long countByIsDeletedFalseAndContentTypeIdIn(List<String> contentTypeIds);

    @Query(value = """
            SELECT *
            FROM tourism_places
            WHERE is_deleted = false
              AND content_type_id IN ('12', '14', '28')
              AND ST_DWithin(
                    CAST(location AS geography),
                    CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography),
                    :radiusMeters
              )
            ORDER BY ST_Distance(
                    CAST(location AS geography),
                    CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)
              )
            LIMIT :resultLimit
            """, nativeQuery = true)
    List<TourismPlace> findNearbyOfficialTourismPlaces(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("resultLimit") int resultLimit
    );

    @Query(value = """
            SELECT *
            FROM tourism_places
            WHERE is_deleted = false
              AND content_type_id IN ('12', '14', '28')
              AND (
                    LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(address, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
              AND ST_DWithin(
                    CAST(location AS geography),
                    CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography),
                    :radiusMeters
              )
            ORDER BY ST_Distance(
                    CAST(location AS geography),
                    CAST(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS geography)
              )
            LIMIT :resultLimit
            """, nativeQuery = true)
    List<TourismPlace> searchNearbyOfficialTourismPlaces(
            @Param("keyword") String keyword,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters,
            @Param("resultLimit") int resultLimit
    );

    @Query(value = """
            SELECT *
            FROM tourism_places
            WHERE is_deleted = false
              AND content_type_id IN ('12', '14', '28')
              AND (
                    LOWER(title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(address, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY
              CASE
                WHEN LOWER(title) = LOWER(:keyword) THEN 0
                WHEN LOWER(title) LIKE LOWER(CONCAT(:keyword, '%')) THEN 1
                ELSE 2
              END,
              title
            LIMIT :resultLimit
            """, nativeQuery = true)
    List<TourismPlace> searchOfficialTourismPlacesByKeyword(
            @Param("keyword") String keyword,
            @Param("resultLimit") int resultLimit
    );
}
