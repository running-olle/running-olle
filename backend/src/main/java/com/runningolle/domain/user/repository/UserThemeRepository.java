package com.runningolle.domain.user.repository;

import com.runningolle.domain.user.entity.UserTheme;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserThemeRepository extends JpaRepository<UserTheme, UUID> {

    void deleteAllByUserId(UUID userId);

    @EntityGraph(attributePaths = "theme")
    List<UserTheme> findAllByUserId(UUID userId);
}
