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

    /**
     * Add expense with full details: item, amount, optional merchant and category
     * @param date optional, defaults to now if not provided
     */
    @PostMapping("/add")
    public String addExpense(
            @RequestParam String item,
            @RequestParam double amount,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String date
    ) {
        try {
            // Default values
            if (merchant == null) merchant = "";
            if (category == null || category.isEmpty()) category = "Other";
            if (date == null || date.isEmpty()) {
                date = java.time.LocalDate.now().toString(); // Only date, no time
            }

            googleSheetsService.addExpense(date, item, amount, merchant, category);

            StringBuilder sb = new StringBuilder();
            sb.append("✅ Expense added:\n");
            sb.append("📝 Item: ").append(item).append("\n");
            sb.append("💰 Price: RM").append(amount).append("\n");
            if (!merchant.isEmpty()) sb.append("🏪 Merchant: ").append(merchant).append("\n");
            if (!category.isEmpty()) sb.append("📂 Category: ").append(category).append("\n");

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Failed to add expense: " + e.getMessage();
        }
    }
}
