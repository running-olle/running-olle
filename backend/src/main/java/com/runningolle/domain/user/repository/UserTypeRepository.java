package com.runningolle.domain.user.repository;

import com.runningolle.domain.user.entity.UserType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTypeRepository extends JpaRepository<UserType, UUID> {
    Optional<UserType> findByCode(String code);
}
