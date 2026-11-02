package org.cartscheduler.repository;

import java.util.Optional;
import org.cartscheduler.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsernameIgnoreCase(String username);

    Optional<AdminUser> findByEmailIgnoreCase(String email);
}
