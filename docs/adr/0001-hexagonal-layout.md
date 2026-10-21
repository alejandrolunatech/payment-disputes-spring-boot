# ADR 0001: Hexagonal layout with a plain-Java core

Status: accepted, October 2026

## Context

The interesting rules here (the lifecycle, the dispute policy, when to trust a model) must be easy to read and test, and must survive a change of database, web framework or model vendor.

## Decision

Three layers, in packages:

- `domain`: the `Dispute` aggregate, its status machine, the policy, and value types. Plain Java only.
- `application`: the use cases (`DisputeService`, `TriageService`) and the ports they need (`DisputeRepository`, `TransactionCatalog`, `TriageModel`, `ModelCallLog`). No Spring annotations.
- `adapter`: `in.web` (REST), `out.persistence` (JPA), `out.llm` (Spring AI and the scripted stand-in). `config` wires them.

`ArchitectureTest` enforces the direction of every dependency. The one library the application layer uses is Resilience4j's circuit breaker, a plain object with no framework behind it.

## Consequences

- Use-case tests run in milliseconds with hand-written fakes, no Spring context.
- Mapping between JPA entities and the domain is written by hand in `JpaDisputeRepository`. It is more code than annotating the domain, and it keeps JPA's rules (no-arg constructors, mutable fields, lazy proxies) out of the domain.
- Transactions live in the persistence adapter, one per write. See ADR 0003.
