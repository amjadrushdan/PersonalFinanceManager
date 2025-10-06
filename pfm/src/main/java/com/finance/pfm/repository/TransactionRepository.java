package com.finance.pfm.repository;

import com.finance.pfm.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Later you can add custom queries e.g. findByCategory, findByCreatedAtBetween, etc.
}
