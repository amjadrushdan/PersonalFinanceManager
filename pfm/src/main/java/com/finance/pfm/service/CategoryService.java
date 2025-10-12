package com.finance.pfm.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    public String classifyExpense(String itemName) {
        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(20));

            String prompt = """
                    You are a financial assistant that classifies expense items into a predefined category.
                    Choose only one category from this list:
                    Food, Transport, Shopping, Bills, Entertainment, Groceries, Fuel, Other.

                    Example:
                    - "Nasi Lemak" → Food
                    - "Grab ride" → Transport
                    - "Petronas fuel" → Fuel
                    - "Netflix subscription" → Entertainment

                    Expense: "%s"
                    Reply only with the category name.
                    """.formatted(itemName);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(new ChatMessage("user", prompt)))
                    .maxTokens(10)
                    .temperature(0.2)
                    .build();

            var response = service.createChatCompletion(request);

            Optional<String> result = response.getChoices().stream()
                    .map(choice -> choice.getMessage().getContent())
                    .filter(content -> content != null && !content.isBlank())
                    .findFirst();

            return result.orElse("Other").trim();

        } catch (Exception e) {
            System.err.println("Error classifying expense: " + e.getMessage());
            e.printStackTrace();
            return "Other";
        }
    }
}
