package com.runningolle.domain.user.repository;

import com.runningolle.domain.user.entity.Theme;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThemeRepository extends JpaRepository<Theme, UUID> {
    Optional<Theme> findByCode(String code);

    List<Theme> findAllByCodeIgnoreCase(String code);
}
