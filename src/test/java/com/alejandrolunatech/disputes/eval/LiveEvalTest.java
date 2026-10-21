package com.alejandrolunatech.disputes.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.alejandrolunatech.disputes.adapter.out.llm.SpringAiTriageModel;
import java.io.IOException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * The real-model run. Off unless asked for, and it reads the key only from the environment:
 * {@code op run --env-file=op.env -- ./mvnw test -Dtest=LiveEvalTest -Deval.live=true}.
 * 40 calls; see docs/evals/README.md for the cost estimate.
 */
@EnabledIfSystemProperty(named = "eval.live", matches = "true")
class LiveEvalTest {

    @Test
    void runsTheCasesAgainstARealModel() throws IOException {
        String key = System.getenv("OPENROUTER_API_KEY");
        assertThat(key).as("OPENROUTER_API_KEY must be set").isNotBlank();
        String model = System.getProperty("eval.model", "anthropic/claude-haiku-5.5");
        String baseUrl = System.getProperty("eval.baseUrl", "https://openrouter.ai/api/v1");

        var chatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder().baseUrl(baseUrl).apiKey(key).model(model).temperature(0.0)
                        .maxRetries(1).build())
                .build();
        var report = TriageEval.run(new SpringAiTriageModel(chatModel, model), model, 0.75, TriageEval.cases());

        var file = TriageEval.write(report, "live-" + model.replace('/', '_') + ".md",
                "Triage eval: " + model + ", " + LocalDate.now(),
                "Real model through OpenRouter, temperature 0, one run. Synthetic cases written for this repo.");
        System.out.println("Report written to " + file.toAbsolutePath());
        assertThat(report.results()).hasSize(40);
    }
}
