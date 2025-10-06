package com.finance.pfm.controller;

import com.finance.pfm.service.GoogleSheetsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sheets")
public class GoogleSheetsController {

    private final GoogleSheetsService googleSheetsService;

    public GoogleSheetsController(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @PostMapping("/add")
    public String addExpense(@RequestParam String description, @RequestParam double amount) {
        try {
            googleSheetsService.addExpense(description, amount);
            return "✅ Expense added: " + description + " - RM" + amount;
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Failed: " + e.getMessage();
        }
    }
}
