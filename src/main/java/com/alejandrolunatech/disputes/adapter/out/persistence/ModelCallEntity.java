package com.alejandrolunatech.disputes.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "model_calls")
class ModelCallEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "called_at", nullable = false)
    Instant calledAt;

    @Column(nullable = false)
    String model;

    @Column(name = "input_tokens", nullable = false)
    int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    int outputTokens;

    @Column(name = "latency_ms", nullable = false)
    long latencyMs;

    @Column(nullable = false)
    String outcome;

    String detail;

    protected ModelCallEntity() {
    }
}
