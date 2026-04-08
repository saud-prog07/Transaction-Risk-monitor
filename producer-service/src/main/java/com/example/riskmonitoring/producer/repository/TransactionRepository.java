package com.example.riskmonitoring.producer.repository;

import com.example.riskmonitoring.producer.domain.SubmittedTransaction;
import java.util.Optional;

public interface TransactionRepository {

    SubmittedTransaction save(SubmittedTransaction transaction);

    Optional<SubmittedTransaction> findByTransactionId(String transactionId);
}