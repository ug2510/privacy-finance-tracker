package com.privacyfinance.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test: verifies the Spring application context loads successfully.
 *
 * <p>We override the Gemini API key with a dummy value so this test can run
 * in CI/CD (GitHub Actions) without a real key.
 */
@SpringBootTest(classes = FinanceTrackerApplication.class)
@TestPropertySource(properties = {
        "GEMINI_API_KEY=test-dummy-key-for-context-load",
        "spring.ai.googleai.gemini.api-key=test-dummy-key-for-context-load"
})
class FinanceTrackerApplicationTests {

    @Test
    void contextLoads() {
        // If this passes, all beans wired correctly
    }
}
