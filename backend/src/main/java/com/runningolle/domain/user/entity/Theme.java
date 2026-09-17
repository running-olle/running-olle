package com.runningolle.domain.user.entity;

import com.runningolle.domain.user.enums.ThemeCode;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "themes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.FIELD)
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    public static Theme create(String code, String name) {
        Theme theme = new Theme();
        theme.code = code;
        theme.name = name;
        return theme;
    }

    public static Theme create(ThemeCode code) {
        return create(code.name(), code.getDisplayName());
    }

    public void synchronize(ThemeCode code) {
        this.code = code.name();
        this.name = code.getDisplayName();
    }
}
