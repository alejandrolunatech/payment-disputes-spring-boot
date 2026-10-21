package com.alejandrolunatech.disputes.config;

import com.alejandrolunatech.disputes.adapter.out.llm.ScriptedTriageModel;
import com.alejandrolunatech.disputes.adapter.out.llm.SpringAiTriageModel;
import com.alejandrolunatech.disputes.application.DisputeService;
import com.alejandrolunatech.disputes.application.TriageService;
import com.alejandrolunatech.disputes.application.port.DisputeRepository;
import com.alejandrolunatech.disputes.application.port.ModelCallLog;
import com.alejandrolunatech.disputes.application.port.TransactionCatalog;
import com.alejandrolunatech.disputes.application.port.TriageModel;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Clock;
import java.time.Duration;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the plain-Java application classes. They carry no Spring annotations, so this is the
 * one place that decides which model, clock and breaker they get.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TriageProperties.class)
class DisputesConfiguration {

    /** Ticks in microseconds, as PostgreSQL stores them, so a read returns the same instant that was written. */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }

    @Bean
    TriageModel triageModel(TriageProperties properties, ObjectProvider<ChatModel> chatModel) {
        return switch (properties.model()) {
            case SCRIPTED -> new ScriptedTriageModel();
            case OPENAI -> new SpringAiTriageModel(chatModel.getIfAvailable(() -> {
                throw new IllegalStateException(
                        "disputes.triage.model is openai but no chat model is configured: set spring.ai.model.chat=openai");
            }), properties.modelName());
        };
    }

    @Bean
    CircuitBreaker triageCircuitBreaker(TriageProperties properties) {
        TriageProperties.Breaker settings = properties.breaker();
        return CircuitBreaker.of("triage-model", CircuitBreakerConfig.custom()
                .failureRateThreshold(settings.failureRatePercent())
                .slidingWindowSize(settings.window())
                .minimumNumberOfCalls(settings.minimumCalls())
                .waitDurationInOpenState(settings.openFor())
                .build());
    }

    @Bean
    TriageService triageService(TriageModel model, ModelCallLog callLog, CircuitBreaker triageCircuitBreaker,
                                TriageProperties properties, Clock clock) {
        return new TriageService(model, callLog, triageCircuitBreaker, properties.minConfidence(), clock);
    }

    @Bean
    DisputeService disputeService(DisputeRepository disputes, TransactionCatalog transactions, TriageService triage,
                                  Clock clock) {
        return new DisputeService(disputes, transactions, triage, clock);
    }
}
