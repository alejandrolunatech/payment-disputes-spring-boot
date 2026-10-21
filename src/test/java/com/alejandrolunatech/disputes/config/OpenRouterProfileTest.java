package com.alejandrolunatech.disputes.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.alejandrolunatech.disputes.TestcontainersConfiguration;
import com.alejandrolunatech.disputes.adapter.out.llm.SpringAiTriageModel;
import com.alejandrolunatech.disputes.application.port.TriageModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** The real-model profile wires up without calling anything. The key is a dummy and is never used. */
@SpringBootTest(properties = "OPENROUTER_API_KEY=dummy-not-a-key")
@ActiveProfiles("openrouter")
@Import(TestcontainersConfiguration.class)
class OpenRouterProfileTest {

    @Autowired
    TriageModel model;

    @Test
    void usesTheSpringAiAdapter() {
        assertThat(model).isInstanceOf(SpringAiTriageModel.class);
    }
}
