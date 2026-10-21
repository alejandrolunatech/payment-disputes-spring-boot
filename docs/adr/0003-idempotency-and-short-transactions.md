# ADR 0003: Idempotency keys, and no transaction across the model call

Status: accepted, October 2026

## Context

Clients retry after timeouts. A retried "open dispute" must not create a second dispute or pay for a second model call. The model call can also take seconds, and holding a database transaction and connection open for that long does not scale.

## Decision

- `POST /api/disputes` requires an `Idempotency-Key` header. The service stores the key with a SHA-256 fingerprint of the request.
- A request with a known key and the same fingerprint returns the stored dispute. A known key with a different fingerprint is refused with `422`, as the IETF Idempotency-Key draft suggests.
- Two requests with the same new key can race. The unique constraint on `idempotency_key` lets one insert win; the loser reads the winner and applies the same comparison.
- The use case reads, calls the model, and then writes in one short transaction in the adapter. No transaction is open during the model call.
- Later changes use optimistic locking (`@Version`). A lost update is a `409`.

## Consequences

- In a race, both requests may call the model once. That is accepted: it is rare, and it costs a little money, not correctness.
- Keys are kept forever in this version. A real service would expire them after a retention period.
