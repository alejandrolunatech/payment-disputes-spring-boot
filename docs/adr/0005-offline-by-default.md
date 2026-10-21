# ADR 0005: Offline by default

Status: accepted, October 2026

## Context

Anyone who clones the repo, and every CI run, should be able to build, test and run the service without an API key, without cost, and with the same result every time.

## Decision

- The default configuration turns off Spring AI's model auto-configuration (`spring.ai.model.chat: none`) and uses `ScriptedTriageModel`, a deterministic stand-in that is named and documented as not a model.
- The `openrouter` profile switches to the real adapter. The key comes only from the environment (`op run` with 1Password references in `op.env`), never from a file in the repo.
- The real-model eval (`LiveEvalTest`) only runs when asked for with `-Deval.live=true`.

## Consequences

- CI proves the code paths and the harness, not model quality. Real-model results are run by hand and committed with their date, model and settings.
- The scripted stand-in's numbers are always labelled as such, so they cannot be mistaken for model results.
