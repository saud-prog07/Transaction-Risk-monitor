package com.example.riskmonitoring.producer.repository;

import com.example.riskmonitoring.producer.domain.SubmittedTransaction;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<String, SubmittedTransaction> transactions = new ConcurrentHashMap<>();

    @Override
    public SubmittedTransaction save(SubmittedTransaction transaction) {
        transactions.put(transaction.getRequest().transactionId(), transaction);
        return transaction;
    }

    @Override
    public Optional<SubmittedTransaction> findByTransactionId(String transactionId) {
        return Optional.ofNullable(transactions.get(transactionId));
    }
}