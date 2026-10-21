package com.alejandrolunatech.disputes.eval;

import com.alejandrolunatech.disputes.application.PiiMasker;
import com.alejandrolunatech.disputes.application.TriageService;
import com.alejandrolunatech.disputes.application.port.ModelCallLog;
import com.alejandrolunatech.disputes.application.port.TriageModel;
import com.alejandrolunatech.disputes.domain.Money;
import com.alejandrolunatech.disputes.domain.Transaction;
import com.alejandrolunatech.disputes.domain.Triage;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the labelled cases through the real {@link TriageService}, so what is measured is the
 * whole decision (model plus checks plus fallback), not the model alone.
 */
final class TriageEval {

    /** One labelled case. {@code expected} is a reason code, or REVIEW when a person should decide. */
    record Case(String id, String merchant, String amount, String expected, String statement) {
    }

    record Result(Case testCase, Triage triage, long latencyMillis) {

        String outcome() {
            return switch (triage) {
                case Triage.Decided decided -> decided.reasonCode().name();
                case Triage.NeedsReview review -> "REVIEW";
            };
        }

        boolean automated() {
            return triage instanceof Triage.Decided;
        }

        boolean correct() {
            return outcome().equals(testCase.expected());
        }
    }

    record Report(String model, List<Result> results, List<ModelCallLog.Entry> calls) {

        long automated() {
            return results.stream().filter(Result::automated).count();
        }

        /** Decided without a person, and wrong: the error that costs money. */
        List<Result> wrongAutomated() {
            return results.stream().filter(r -> r.automated() && !r.correct()).toList();
        }

        long correct() {
            return results.stream().filter(Result::correct).count();
        }

        long reviewCasesSentToReview() {
            return results.stream().filter(r -> r.testCase().expected().equals("REVIEW") && !r.automated()).count();
        }

        long reviewCases() {
            return results.stream().filter(r -> r.testCase().expected().equals("REVIEW")).count();
        }

        long tokens(boolean input) {
            return calls.stream().mapToLong(c -> input ? c.inputTokens() : c.outputTokens()).sum();
        }

        long latencyPercentile(int percentile) {
            List<Long> sorted = calls.stream().map(ModelCallLog.Entry::latencyMillis).sorted().toList();
            if (sorted.isEmpty()) {
                return 0;
            }
            int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
            return sorted.get(Math.max(0, index));
        }

        String markdown(String heading, String caveat) {
            var out = new StringBuilder();
            out.append("# ").append(heading).append("\n\n");
            out.append("> ").append(caveat).append("\n\n");
            out.append("Model: `").append(model).append("`. Cases: ").append(results.size())
                    .append(" synthetic statements (`src/test/resources/evals/triage-cases.jsonl`).\n\n");
            out.append("| Measure | Value |\n|---|---|\n");
            out.append("| Correct outcome (reason, or sent to a person when expected) | ")
                    .append(correct()).append(" / ").append(results.size()).append(" |\n");
            out.append("| Decided without a person | ").append(automated()).append(" / ").append(results.size()).append(" |\n");
            out.append("| Decided without a person, and wrong | ").append(wrongAutomated().size()).append(" |\n");
            out.append("| Unclear cases sent to a person | ").append(reviewCasesSentToReview()).append(" / ")
                    .append(reviewCases()).append(" |\n");
            out.append("| Tokens in / out | ").append(tokens(true)).append(" / ").append(tokens(false)).append(" |\n");
            out.append("| Latency p50 / p95 | ").append(latencyPercentile(50)).append(" ms / ")
                    .append(latencyPercentile(95)).append(" ms |\n\n");
            out.append("| Case | Expected | Outcome | Decided by | Confidence |\n|---|---|---|---|---|\n");
            for (Result r : results) {
                String by = r.triage() instanceof Triage.Decided d ? d.source().name() : "person";
                String confidence = r.triage() instanceof Triage.Decided d ? "%.2f".formatted(d.confidence()) : "";
                out.append("| ").append(r.testCase().id()).append(" | ").append(r.testCase().expected()).append(" | ")
                        .append(r.correct() ? "" : "**").append(r.outcome()).append(r.correct() ? "" : "**")
                        .append(" | ").append(by).append(" | ").append(confidence).append(" |\n");
            }
            return out.toString();
        }
    }

    static List<Case> cases() {
        JsonMapper json = JsonMapper.builder().build();
        try (InputStream in = Objects.requireNonNull(TriageEval.class.getResourceAsStream("/evals/triage-cases.jsonl"))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.isBlank())
                    .map(line -> json.readValue(line, Case.class))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Report run(TriageModel model, String modelName, double minConfidence, List<Case> cases) {
        var calls = new ArrayList<ModelCallLog.Entry>();
        var triage = new TriageService(model, calls::add, CircuitBreaker.ofDefaults("eval"), minConfidence,
                Clock.systemUTC());
        var results = new ArrayList<Result>();
        for (Case c : cases) {
            var transaction = new Transaction("EVAL-" + c.id(), Money.eur(c.amount()), c.merchant(), LocalDate.now());
            long started = System.nanoTime();
            Triage outcome = triage.triage(PiiMasker.mask(c.statement()), transaction);
            results.add(new Result(c, outcome, (System.nanoTime() - started) / 1_000_000));
        }
        return new Report(modelName, results, calls);
    }

    static Path write(Report report, String fileName, String heading, String caveat) throws IOException {
        Path file = Path.of("target", "evals", fileName);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, report.markdown(heading, caveat));
    }

    private TriageEval() {
    }
}
