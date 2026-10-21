# Evals

The eval set is [`triage-cases.jsonl`](../../src/test/resources/evals/triage-cases.jsonl): 40 synthetic statements written for this repo, each labelled with the reason a careful person would pick, or `REVIEW` when a person should decide. Seven are in Dutch or Spanish. Six are `REVIEW` cases: vague complaints, a claim of more money than was paid, and a prompt-injection attempt.

[`TriageEval`](../../src/test/java/com/alejandrolunatech/disputes/eval/TriageEval.java) runs each case through the real `TriageService` (masking, model, checks, fallback) and writes a Markdown report to `target/evals/`.

## What is measured

- **Decided without a person, and wrong.** The costly error. It should be zero or close to it.
- **Decided without a person.** How much work the model saves.
- **Unclear cases sent to a person.** Whether the checks catch what they should.
- Tokens and latency per call.

## Runs

| Date | Model | Settings | Correct | Automated | Automated and wrong | Report |
| --- | --- | --- | --- | --- | --- | --- |
| every CI build | scripted stand-in, **not a model** | threshold 0.75 | 19 / 40 | 13 / 40 | 0 | CI artifact `eval-report-scripted` |
| PENDING | first real-model run planned for November 2026 | | | | | |

## Running it against a real model

Needs an OpenRouter key with a spending limit, in 1Password as `portfolio-openrouter` (see [`op.env`](../../op.env)). From the repo folder:

```bash
op run --env-file=op.env -- ./mvnw test -Dtest=LiveEvalTest -Deval.live=true
```

Optional: `-Deval.model=<openrouter model id>`. Each case is one call of roughly 350 input and 30 output tokens, so a run of 40 cases is about 15,000 tokens. On a small model that is a few cents.

After a run, copy the report from `target/evals/` to this folder, add a row above, and write one paragraph on what it shows, including the wrong decisions.
