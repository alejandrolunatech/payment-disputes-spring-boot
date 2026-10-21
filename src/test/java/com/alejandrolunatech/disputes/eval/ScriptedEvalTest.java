package com.alejandrolunatech.disputes.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.alejandrolunatech.disputes.adapter.out.llm.ScriptedTriageModel;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/**
 * Runs the eval harness in CI on the scripted stand-in. It proves the harness works and the
 * safety checks hold; its numbers say nothing about a real model.
 */
class ScriptedEvalTest {

    @Test
    void theHarnessRunsEveryCaseAndWritesAReport() throws IOException {
        var cases = TriageEval.cases();
        var report = TriageEval.run(new ScriptedTriageModel(), ScriptedTriageModel.NAME, 0.75, cases);

        assertThat(cases).hasSize(40);
        assertThat(report.results()).hasSize(40);
        assertThat(report.calls()).hasSize(40);
        var file = TriageEval.write(report, "scripted.md", "Triage eval: scripted stand-in",
                "NOT A MODEL. This run uses the deterministic stand-in to test the harness. "
                        + "Real-model results are in docs/evals/.");
        assertThat(Files.readString(file)).contains("| rv-04 | REVIEW | REVIEW |");
    }

    @Test
    void aClaimAboveThePaidAmountIsNeverDecidedAutomatically() {
        var overClaim = TriageEval.cases().stream().filter(c -> c.id().equals("rv-04")).toList();

        var report = TriageEval.run(new ScriptedTriageModel(), ScriptedTriageModel.NAME, 0.75, overClaim);

        assertThat(report.results().getFirst().automated()).isFalse();
    }
}
