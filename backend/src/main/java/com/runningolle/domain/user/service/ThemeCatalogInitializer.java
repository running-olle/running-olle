package com.runningolle.domain.user.service;

import com.runningolle.domain.user.entity.Theme;
import com.runningolle.domain.user.enums.ThemeCode;
import com.runningolle.domain.user.repository.ThemeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThemeCatalogInitializer implements ApplicationRunner {

    private final ThemeRepository themeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (ThemeCode code : ThemeCode.values()) {
            List<Theme> matches = themeRepository.findAllByCodeIgnoreCase(code.name());
            Theme canonical = matches.stream()
                    .filter(theme -> code.name().equals(theme.getCode()))
                    .findFirst()
                    .orElseGet(() -> matches.isEmpty() ? Theme.create(code) : matches.get(0));

            canonical.synchronize(code);
            themeRepository.save(canonical);

            if (matches.size() > 1) {
                log.warn("Duplicate theme codes differing only by case were found for {}. Run theme-catalog-migration.sql.", code);
            }
        }
    }
}
