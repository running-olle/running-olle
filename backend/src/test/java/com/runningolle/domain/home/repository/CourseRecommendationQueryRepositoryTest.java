package com.runningolle.domain.home.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.runningolle.domain.course.entity.Course;
import com.runningolle.domain.course.enums.CourseType;
import com.runningolle.domain.course.enums.Difficulty;
import com.runningolle.domain.home.repository.CourseRecommendationQueryRepository.CourseRecommendationCandidate;
import com.runningolle.domain.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(CourseRecommendationQueryRepository.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfSystemProperty(named = "runningolle.external-smoke", matches = "true")
class CourseRecommendationQueryRepositoryTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    @Autowired
    private CourseRecommendationQueryRepository courseRecommendationQueryRepository;

    @Test
    void returnsOnlyPublicAndNotDeletedCourses() {
        User creator = persistUser("creator-public-filter");
        Course visibleCourse = persistCourse(creator, "Visible Course", true, false, 126.5000, 33.5000);
        Course privateCourse = persistCourse(creator, "Private Course", false, false, 126.5100, 33.5100);
        Course deletedCourse = persistCourse(creator, "Deleted Course", true, true, 126.5200, 33.5200);

        List<CourseRecommendationCandidate> candidates =
                courseRecommendationQueryRepository.findRecommendationCandidates(null, null);

        assertThat(candidates)
                .extracting(CourseRecommendationCandidate::courseId)
                .contains(visibleCourse.getId())
                .doesNotContain(privateCourse.getId(), deletedCourse.getId());
        CourseRecommendationCandidate visibleCandidate = candidates.stream()
                .filter(candidate -> candidate.courseId().equals(visibleCourse.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(visibleCandidate.distanceMeters()).isNull();
    }

    @Test
    void calculatesDistanceWhenLocationProvided() {
        User creator = persistUser("creator-distance");
        Course samePointCourse = persistCourse(creator, "Same Point", true, false, 126.5000, 33.5000);
        persistCourse(creator, "Far Point", true, false, 126.7000, 33.7000);

        List<CourseRecommendationCandidate> candidates =
                courseRecommendationQueryRepository.findRecommendationCandidates(33.5000, 126.5000);

        CourseRecommendationCandidate samePointCandidate = candidates.stream()
                .filter(candidate -> candidate.courseId().equals(samePointCourse.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(samePointCandidate.distanceMeters()).isNotNull();
        assertThat(samePointCandidate.distanceMeters()).isLessThan(1.0d);
    }

    @Test
    void returnsNullDistanceWhenLocationMissing() {
        User creator = persistUser("creator-no-location");
        persistCourse(creator, "No Location Course", true, false, 126.5000, 33.5000);

        List<CourseRecommendationCandidate> candidates =
                courseRecommendationQueryRepository.findRecommendationCandidates(null, null);

        assertThat(candidates)
                .filteredOn(candidate -> "No Location Course".equals(candidate.courseName()))
                .singleElement()
                .satisfies(candidate -> assertThat(candidate.distanceMeters()).isNull());
    }

    private User persistUser(String kakaoId) {
        User user = User.createKakaoUser(kakaoId + "-" + UUID.randomUUID());
        LocalDateTime now = LocalDateTime.of(2026, 8, 30, 0, 0);
        ReflectionTestUtils.setField(user, "createdAt", now);
        ReflectionTestUtils.setField(user, "updatedAt", now);
        entityManager.persist(user);
        return user;
    }

    private Course persistCourse(
            User creator,
            String name,
            boolean isPublic,
            boolean isDeleted,
            double longitude,
            double latitude
    ) {
        var route = GEOMETRY_FACTORY.createLineString(new Coordinate[]{
                new Coordinate(longitude, latitude),
                new Coordinate(longitude + 0.005, latitude + 0.005)
        });
        route.setSRID(4326);

        var startPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        startPoint.setSRID(4326);

        Course course = Course.create(
                creator,
                name,
                "Recommendation query repository test course",
                CourseType.RUNNING_COURSE,
                new BigDecimal("5.00"),
                45,
                new BigDecimal("40.00"),
                Difficulty.MID,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                route,
                startPoint,
                null,
                isPublic
        );
        ReflectionTestUtils.setField(course, "isDeleted", isDeleted);
        LocalDateTime now = LocalDateTime.of(2026, 8, 30, 0, 0);
        ReflectionTestUtils.setField(course, "createdAt", now);
        ReflectionTestUtils.setField(course, "updatedAt", now);
        entityManager.persist(course);
        entityManager.flush();
        entityManager.clear();
        return course;
    }
}
