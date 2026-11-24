package com.privacyfinance.backend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.googleai.gemini.GoogleAiGeminiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Spring AI {@link ChatClient} bean backed by Google Gemini.
 *
 * <p>The underlying {@link GoogleAiGeminiChatModel} is auto-configured by
 * Spring AI using the properties in {@code application.properties}:
 * <pre>
 *   spring.ai.googleai.gemini.api-key=${GEMINI_API_KEY}
 *   spring.ai.googleai.gemini.chat.options.model=gemini-1.5-flash
 * </pre>
 */
@Configuration
public class AiConfig {

    /**
     * Builds a fluent {@link ChatClient} from the auto-configured Gemini model.
     * Inject this bean wherever you need to call Gemini.
     */
    @Bean
    public ChatClient chatClient(GoogleAiGeminiChatModel geminiChatModel) {
        return ChatClient.builder(geminiChatModel).build();
    }
}
