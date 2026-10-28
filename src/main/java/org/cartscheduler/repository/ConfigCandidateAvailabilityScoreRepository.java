package org.cartscheduler.repository;

import java.util.Optional;
import org.cartscheduler.entity.ConfigCandidateAvailabilityScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigCandidateAvailabilityScoreRepository
        extends JpaRepository<ConfigCandidateAvailabilityScore, Long> {
    Optional<ConfigCandidateAvailabilityScore> findByAppearanceCountFromAndAppearanceCountTo(int from, int to);
}
