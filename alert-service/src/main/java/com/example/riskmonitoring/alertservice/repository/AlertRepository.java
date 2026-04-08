package com.example.riskmonitoring.alertservice.repository;

import com.example.riskmonitoring.alertservice.domain.AlertEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<AlertEntity, String> {

    Optional<AlertEntity> findByTransactionId(String transactionId);
}