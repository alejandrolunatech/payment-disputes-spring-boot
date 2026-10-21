package com.alejandrolunatech.disputes.adapter.out.persistence;

import com.alejandrolunatech.disputes.application.port.ModelCallLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Stores each call's model, tokens, latency and outcome. Never the prompt or the customer's text. */
@Component
class JpaModelCallLog implements ModelCallLog {

    private final ModelCallJpaRepository calls;

    JpaModelCallLog(ModelCallJpaRepository calls) {
        this.calls = calls;
    }

    @Override
    @Transactional
    public void record(Entry entry) {
        ModelCallEntity entity = new ModelCallEntity();
        entity.calledAt = entry.at();
        entity.model = entry.model();
        entity.inputTokens = entry.inputTokens();
        entity.outputTokens = entry.outputTokens();
        entity.latencyMs = entry.latencyMillis();
        entity.outcome = entry.outcome().name();
        entity.detail = entry.detail() == null || entry.detail().length() <= 300
                ? entry.detail() : entry.detail().substring(0, 300);
        calls.save(entity);
    }
}
