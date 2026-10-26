package org.cartscheduler.repository;

import org.cartscheduler.entity.ConfigGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigGenerationRepository extends JpaRepository<ConfigGeneration, Short> {
}
