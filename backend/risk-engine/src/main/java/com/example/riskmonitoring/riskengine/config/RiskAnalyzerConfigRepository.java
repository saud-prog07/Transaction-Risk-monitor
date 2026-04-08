package com.example.riskmonitoring.riskengine.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RiskAnalyzerConfig entities.
 * Provides data access methods for risk analyzer configurations.
 */
@Repository
public interface RiskAnalyzerConfigRepository extends JpaRepository<RiskAnalyzerConfig, Long> {

    /**
     * Find configuration by analyzer name
     * @param analyzerName the name of the analyzer
     * @return RiskAnalyzerConfig if found
     */
    Optional<RiskAnalyzerConfig> findByAnalyzerName(String analyzerName);

    /**
     * Find all enabled analyzers
     * @return list of enabled configurations
     */
    List<RiskAnalyzerConfig> findByEnabledTrue();

    /**
     * Find all configurations with optional filter by enabled status
     * @return list of all configurations
     */
    List<RiskAnalyzerConfig> findAll();

    /**
     * Count enabled analyzers
     * @return count of enabled analyzers
     */
    Long countByEnabledTrue();

    /**
     * Check if analyzer name exists
     * @param analyzerName the analyzer name
     * @return true if exists
     */
    boolean existsByAnalyzerName(String analyzerName);
}
