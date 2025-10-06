package com.finance.pfm.repository;

import com.finance.pfm.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findTop5ByOrderByCreatedAtDesc();
}
