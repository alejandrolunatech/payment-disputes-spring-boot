# Working in this repo

A portfolio repo: a payment disputes service in Spring Boot 4 where a language model proposes and code decides. Read `README.md` first, then the ADRs in `docs/adr/`.

- Code is written fresh for this repo. Never copy code, names, rules or domain details from an employer, a client, or any paid task platform. The dispute rules are made up and the README says so.
- Keep the layers: `domain` is plain Java, `application` has no Spring and no adapter, adapters don't know each other. `ArchitectureTest` enforces this.
- Tests never call a network or a real model. The default config is offline (ADR 0005). `LiveEvalTest` runs only with `-Deval.live=true`, and only when the owner asks, because it spends his money.
- Never write a model result that was not produced by a real run. Eval rows in `docs/evals/README.md` carry the date, model and settings.
- Secrets: only `op://` references in `op.env`. No key in any file, test, log or commit.
- Every model call is recorded in `model_calls`; the statement text is never logged.
- Flyway migrations are append-only once merged: add the next version, never edit one.
- Compile target is Java 21; CI also runs Java 25. The build treats warnings as errors. Run `./mvnw verify` (needs Docker) before every push.
- Claude works on a feature branch and opens a pull request. Never push to `main`, never merge.
- `docs/LEARNING-NOTES.md` is the owner's own writing. Don't fill it in.
