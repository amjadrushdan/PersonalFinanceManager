package com.finance.pfm.controller;

import com.finance.pfm.config.TelegramBotConfig;
import com.finance.pfm.service.GoogleSheetsService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBotController extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final GoogleSheetsService googleSheetsService;

    public TelegramBotController(TelegramBotConfig config, GoogleSheetsService googleSheetsService) {
        super(config.getToken());
        this.config = config;
        this.googleSheetsService = googleSheetsService;
        System.out.println("✅ TelegramBotController initialized for bot @" + config.getUsername());
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @PostConstruct
    public void init() {
        System.out.println("🤖 Telegram bot [" + config.getUsername() + "] started with token prefix: "
                + config.getToken().substring(0, 8) + "...");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String messageText = update.getMessage().getText().trim();

            if (messageText.equalsIgnoreCase("/start")) {
                sendMessage(chatId, "👋 Hi! Use this format to log expenses:\nExample: `nasi lemak 5.50`");
            } else {
                handleLogExpense(chatId, messageText);
            }
        }
    }

    private void handleLogExpense(String chatId, String messageText) {
        try {
            // Normalize input
            String input = messageText.toLowerCase().trim();

            // Regex to detect amount with or without "rm"
            // Matches examples: "rm5", "5.00", "rm 12.5", "12.50"
            java.util.regex.Pattern amountPattern = java.util.regex.Pattern.compile("(rm\\s*)?(\\d+(\\.\\d{1,2})?)");
            java.util.regex.Matcher matcher = amountPattern.matcher(input);

            double amount = -1;
            if (matcher.find()) {
                amount = Double.parseDouble(matcher.group(2)); // group(2) = numeric part
            } else {
                sendMessage(chatId, "⚠️ Couldn't detect an amount. Try formats like `nasi lemak 5.50` or `rm5`");
                return;
            }

            // Remove the amount (and 'rm') from the text to get description
            String description = input.replace(matcher.group(), "").trim();

            if (description.isEmpty()) {
                description = "(no description)";
            }

            // Log to Google Sheet
            googleSheetsService.addExpense(description, amount);
            sendMessage(chatId, "✅ Logged to Google Sheet:\n📝 " + description + "\n💰 RM" + amount);

        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "⚠️ Failed to log expense. Try something like:\n`nasi lemak 5.50` or `rm10 lunch`");
        }
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
