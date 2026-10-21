# ADR 0002: The model proposes, code decides

Status: accepted, October 2026

## Context

A language model reads free text far better than keyword rules, especially across languages. It is also sometimes confidently wrong, can be talked into an answer by the text it reads, and can be slow or down. A dispute decides where money goes, so a wrong automatic decision costs more than a slow one.

## Decision

- The model gets masked text and the transaction facts, and returns a proposal: a reason code as a plain string, a confidence, and the amount the statement mentions.
- `TriageService.check` accepts the proposal only if the reason is one we know, the confidence is a valid number at or above `disputes.triage.min-confidence` (0.75), and the mentioned amount is not more than the payment. Anything else goes to the review queue with the reason why.
- When the model is unavailable or the circuit is open, `KeywordRules` answer. When no rule matches, a person decides.
- Every outcome records who decided it: `MODEL`, `RULES` or `REVIEWER`.

## Consequences

- The worst case of a bad model is more work for reviewers, not a wrong decision without a check.
- The checks do not prove the model's reason is right. Evals on labelled cases measure that, and set the threshold. See [docs/evals](../evals/README.md).
- A prompt injection that produces a known reason with high confidence can still pass. The eval set includes such a case so it shows up in the numbers.
