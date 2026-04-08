package com.example.riskmonitoring.alertservice.repository;

import com.example.riskmonitoring.alertservice.domain.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for SystemConfiguration entity
 */
@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {
}
