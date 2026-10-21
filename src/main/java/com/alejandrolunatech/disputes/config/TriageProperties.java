package com.alejandrolunatech.disputes.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings under {@code disputes.triage}. The app refuses to start when one is out of range.
 *
 * @param model          {@code scripted} (offline, no key) or {@code openai} (any OpenAI-compatible API)
 * @param modelName      the name recorded in the call log when the API does not report one
 * @param minConfidence  proposals below this go to a person
 * @param breaker        when to stop calling a failing model, and for how long
 */
@Validated
@ConfigurationProperties("disputes.triage")
public record TriageProperties(@NotNull Model model, @NotBlank String modelName,
                               @DecimalMin("0.0") @DecimalMax("1.0") double minConfidence,
                               @NotNull Breaker breaker) {

    public enum Model { SCRIPTED, OPENAI }

    /**
     * @param failureRatePercent  open the circuit when this share of recent calls failed
     * @param window              how many recent calls count
     * @param minimumCalls        calls needed before the rate is judged
     * @param openFor             how long to skip the model before trying again
     */
    public record Breaker(@Min(1) int failureRatePercent, @Min(1) int window, @Min(1) int minimumCalls,
                          @NotNull Duration openFor) {
    }
}
