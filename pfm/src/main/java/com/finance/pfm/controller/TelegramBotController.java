package com.finance.pfm.controller;

import com.finance.pfm.config.TelegramBotConfig;
import com.finance.pfm.service.GoogleSheetsService;
import com.finance.pfm.service.GoogleDriveService;
import jakarta.annotation.PostConstruct;

import java.util.List;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBotController extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final GoogleSheetsService googleSheetsService;
    private final GoogleDriveService googleDriveService;


    public TelegramBotController(TelegramBotConfig config, GoogleSheetsService googleSheetsService, GoogleDriveService googleDriveService) {
        super(config.getToken());
        this.config = config;
        this.googleSheetsService = googleSheetsService;
        this.googleDriveService = googleDriveService;
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
    String chatId = update.getMessage().getChatId().toString();

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().trim();

            if (messageText.equalsIgnoreCase("/start")) {
                sendMessage(chatId, "👋 Hi! Use this format to log expenses:\nExample: `nasi lemak 5.50`");
            } else {
                handleLogExpense(chatId, messageText);
            }          
        } else if (update.getMessage().hasPhoto()) {
            handleReceiptImage(chatId, update);
        }  
    }

    private void handleLogExpense(String chatId, String messageText) {
        try {
            // Normalize input
            String input = messageText.trim();

            // 1️⃣ Extract price (last numeric value, with or without "RM")
            java.util.regex.Pattern amountPattern = java.util.regex.Pattern.compile("(rm\\s*)?(\\d+(\\.\\d{1,2})?)");
            java.util.regex.Matcher matcher = amountPattern.matcher(input);

            double price = -1;
            if (matcher.find()) {
                price = Double.parseDouble(matcher.group(2));
            } else {
                sendMessage(chatId, "⚠️ Couldn't detect an amount. Try formats like `nasi lemak 5.50` or `rm5`");
                return;
            }

            // 2️⃣ Extract @Merchant and #Category
            java.util.regex.Pattern merchantPattern = java.util.regex.Pattern.compile("@(\\S+)");
            java.util.regex.Pattern categoryPattern = java.util.regex.Pattern.compile("#(\\S+)");

            java.util.regex.Matcher merchantMatcher = merchantPattern.matcher(input);
            java.util.regex.Matcher categoryMatcher = categoryPattern.matcher(input);

            String merchant = merchantMatcher.find() ? merchantMatcher.group(1) : "";
            String category = categoryMatcher.find() ? categoryMatcher.group(1) : "";

            // 3️⃣ Remove amount, @Merchant, #Category to get item description
            String item = input.replaceAll("(rm\\s*)?\\d+(\\.\\d{1,2})?", "")
                            .replaceAll("@\\S+", "")
                            .replaceAll("#\\S+", "")
                            .trim();

            if (item.isEmpty()) item = "(no description)";

            // 4️⃣ Date (current timestamp)
            String date = java.time.LocalDate.now().toString();
            
            // 5️⃣ Log to Google Sheet
            // You may need to update your GoogleSheetsService to accept merchant and category
            googleSheetsService.addExpense(date, item, price, merchant, category);

            // 6️⃣ Feedback to user
            StringBuilder sb = new StringBuilder();
            sb.append("✅ Logged to Google Sheet:\n");
            sb.append("📝 Item: ").append(item).append("\n");
            sb.append("💰 Price: RM").append(price).append("\n");
            if (!merchant.isEmpty()) sb.append("🏪 Merchant: ").append(merchant).append("\n");
            if (!category.isEmpty()) sb.append("📂 Category: ").append(category).append("\n");

            sendMessage(chatId, sb.toString());

        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "⚠️ Failed to log expense. Try something like:\n`latte stroberi 7.5 @Taobin #Coffee`");
        }
    }

    private void handleReceiptImage(String chatId, Update update) {
        try {
            List<PhotoSize> photos = update.getMessage().getPhoto();
            PhotoSize photo = photos.get(photos.size() - 1); // highest resolution

            // Download the file
            org.telegram.telegrambots.meta.api.objects.File telegramFile = execute(new GetFile(photo.getFileId()));
            java.io.File localFile = new java.io.File("downloads/" + telegramFile.getFilePath());
            downloadFile(telegramFile, localFile);

            // Upload to Google Drive
            googleDriveService.uploadFile(localFile);

            sendMessage(chatId, "✅ Receipt uploaded to Google Drive: " + localFile.getName());

        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "⚠️ Failed to upload receipt image.");
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
