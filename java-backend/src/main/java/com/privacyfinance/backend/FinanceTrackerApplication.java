package com.privacyfinance.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Privacy Finance Tracker backend.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Expose REST APIs consumed by the React Native mobile app over local Wi-Fi.</li>
 *   <li>Persist financial transactions in a local SQLite database (no cloud).</li>
 *   <li>Use Spring AI + Gemini to auto-categorise raw merchant names from SMS alerts.</li>
 *   <li>Delegate CAS PDF parsing to the Python microservice via HTTP.</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling   // needed for future scheduled budget-summary jobs
public class FinanceTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceTrackerApplication.class, args);
    }
}
