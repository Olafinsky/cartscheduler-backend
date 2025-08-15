package org.cartscheduler.repository;

import org.cartscheduler.entity.ParticipantAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ParticipantAccessTokenRepository extends JpaRepository<ParticipantAccessToken, Long> {
    @Query("SELECT pat FROM ParticipantAccessToken pat WHERE pat.token = ?1 AND pat.expiresAt >= current_timestamp")
    ParticipantAccessToken findAccessForToken(String token);
}
