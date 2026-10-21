# ADR 0004: Spring Boot 4 and Spring AI 2 on Java 21

Status: accepted, October 2026

## Context

Spring Boot 4 (Spring Framework 7, Jakarta EE 11, Jackson 3) is the current generation, and Spring AI 2 is built for it. Many teams are still on Boot 3 and planning the move, so working code on Boot 4 is useful to show.

## Decision

- Spring Boot 4.1 with its modular starters (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, `spring-boot-starter-webmvc-test`).
- Spring AI 2.0 for the model call, behind the `TriageModel` port.
- Testcontainers 2 with `@ServiceConnection`, so tests and `spring-boot:test-run` need no hand-written connection settings.
- Java 21 as the compile target; CI also builds and tests on Java 25.

## Consequences

- Some Boot 3 habits change: `@MockitoBean` instead of `@MockBean`, the new test auto-configuration packages, Jackson 3's `tools.jackson` packages, and controller parameter validation built into Spring MVC.
- Spring AI moves fast. The adapter is one class, so a breaking change there stays in one place.
