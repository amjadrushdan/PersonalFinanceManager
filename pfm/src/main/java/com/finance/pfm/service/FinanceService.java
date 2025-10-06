package com.finance.pfm.service;

import com.finance.pfm.model.Expense;
import com.finance.pfm.model.OcrResult;
import com.finance.pfm.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FinanceService {

    private final ExpenseRepository expenseRepository;

    public FinanceService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public Expense logExpense(String input) {
        Expense exp = new Expense();

        // Normalize input
        String text = input.trim().toLowerCase();

        if (text.startsWith("rm")) {
            // Case: starts with "rm"
            String[] parts = text.split("\\s+", 2); // ["rm10", "nasi lemak"]
            String amountStr = parts[0].substring(2); // remove "rm"

            try {
                exp.setAmount(Double.parseDouble(amountStr));
            } catch (NumberFormatException e) {
                exp.setAmount(0.0);
            }

            exp.setDescription(parts.length > 1 ? parts[1] : "Unknown expense");

        } else {
            // Case: just number at the start, e.g. "10 nasi lemak"
            String[] parts = text.split("\\s+", 2);
            try {
                exp.setAmount(Double.parseDouble(parts[0]));
                exp.setDescription(parts.length > 1 ? parts[1] : "Unknown expense");
            } catch (NumberFormatException e) {
                // Fallback: could not parse number, treat whole thing as description
                exp.setAmount(0.0);
                exp.setDescription(text);
            }
        }

        return expenseRepository.save(exp);
    }

    public List<Expense> getRecentExpenses() {
        return expenseRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public Expense logExpenseFromOcr(OcrResult ocr) {
        Expense exp = new Expense();
        exp.setAmount(ocr.getAmount() == null ? 0.0 : ocr.getAmount());
        exp.setDescription(ocr.getDescription() == null ? "receipt" : ocr.getDescription());
        exp.setCreatedAt(java.time.LocalDateTime.now());
        exp.setConfidence(ocr.getConfidence());
        return expenseRepository.save(exp);
    }
}
