package org.cartscheduler.repository;

import org.cartscheduler.entity.ConfigCandidateUniquenessScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigCandidateUniquenessScoreRepository
        extends JpaRepository<ConfigCandidateUniquenessScore, Long> {
}
