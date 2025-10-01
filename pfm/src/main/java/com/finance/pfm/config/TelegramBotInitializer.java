package com.finance.pfm.config;

import com.finance.pfm.controller.TelegramBotController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotInitializer {

    private final TelegramBotController telegramBotController;

    public TelegramBotInitializer(TelegramBotController telegramBotController) {
        this.telegramBotController = telegramBotController;
    }

    @Bean
    public TelegramBotsApi telegramBotsApi() throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(telegramBotController);
        return botsApi;
    }
}
