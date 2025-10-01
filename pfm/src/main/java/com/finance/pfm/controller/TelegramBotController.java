package com.finance.pfm.controller;

import com.finance.pfm.config.TelegramBotConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBotController extends TelegramLongPollingBot {

    private final TelegramBotConfig config;

    public TelegramBotController(TelegramBotConfig config) {
        super(config.getToken()); // Pass token to parent
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getUsername();
    }

    @PostConstruct
    public void init() {
        System.out.println("✅ Telegram bot [" + config.getUsername() + "] is running with token: "
                + config.getToken().substring(0, 8) + "...");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String messageText = update.getMessage().getText();

            System.out.println("📩 Received from Telegram: " + messageText);

            SendMessage message = new SendMessage(chatId, "You said: " + messageText);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}
