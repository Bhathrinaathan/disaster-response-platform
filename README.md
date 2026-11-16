# Response to City Disasters: an LLM-assisted Disaster Management Platform

A multi-user platform for reporting, validating and responding to urban disasters, built by a ten-person team using Extreme Programming over two semesters of the Advanced Software Engineering module, MSc Computer Science, Trinity College Dublin (2025–26).

This repository holds **my contribution to the backend** — the automated disaster-summary subsystem — plus an overview of the full system. The complete team codebase lives in the module's private GitLab and is not reproduced here.

## The platform

```
Civilians ─ report incidents (form or voice), view shelters & evacuation routes
                 │
                 ▼
Spring Boot API ─ validates reports via multi-source aggregation + confidence scoring
                 │  ├─ Kafka ingestion of incoming reports
                 │  ├─ Emergency Response Team (ERT) resource-allocation engine
                 │  └─ LLM-backed summary & report generation  ◀── this repo
                 ▼
Authorities ─ approve allocations, dispatch units across Garda / Fire Brigade / HSE / Council,
              publish civilian-facing summaries
```

**Stack:** Java, Spring Boot 3, PostgreSQL, Apache Kafka, React + TypeScript (Vite), AWS, GitHub Actions CI/CD. Process: Extreme Programming practices across a ten-person team.

## What I built

### 1. Resource-allocation engine for Emergency Response Teams
Computes the vehicles and crews a disaster needs from its type, severity and scale; routes the proposal through authority approval; then allocates the nearest available units across the four responding agencies, substituting across departments when one is exhausted. (Lives in the team repository; summarised here because the summary service below consumes its output.)

### 2. LLM-backed disaster-summary pipeline (code in `src/`)

| Class | Responsibility |
|---|---|
| `DataAggregationService` | Collects everything known about a report: the database record (required), social-media signals, weather and external feeds, and current resource allocations (each optional). A failing optional source is logged and skipped so a summary can still be produced; a failing primary source raises `DataAggregationException`. |
| `LLMIntegrationService` | Builds a structured prompt from the aggregated data, calls the configured chat-completion endpoint with bounded retries, and parses the response into two audiences: an **authority summary** (operational detail, resource status) and a **civilian summary** (plain-language guidance). Failures surface as `LLMServiceException`. |
| `ReportGenerationService` | Renders both summaries to self-contained HTML documents (openable directly in Word, no extra dependencies), writes them to the configured output directory and persists a `GeneratedSummary` row in `DRAFT` state for an authority to review and publish. |
| `GeneratedSummary` / `GeneratedSummaryRepository` | JPA entity and Spring Data repository for generated summaries, their document paths, author and publication state. |
| `*Exception` | Typed exceptions so the controller layer can map aggregation, LLM and report failures to distinct HTTP responses. |

Configuration is externalised through `application.properties` (`llm.api.url`, `llm.model`, `llm.max.retries`, `report.output.directory`, external API URLs), so the same code runs against a mock endpoint in tests and a real provider in deployment.

### 3. AI rerouting tests and cross-service integration
Wrote the test cases for the AI-assisted evacuation rerouting feature and the integration tests that exercise the report → validation → allocation → summary path end to end.

## Design decisions worth noting

- **Degrade, don't fail.** A disaster summary that is missing the weather feed is still useful; one that is missing the report itself is not. The aggregation service encodes exactly that distinction.
- **Two audiences, one call.** Asking the model for authority and civilian summaries in a single structured response keeps them consistent with each other and halves token cost.
- **Human in the loop.** Generated reports are saved as drafts and never published automatically; an authority user reviews and releases them.

## Layout

```
src/main/java/com/disasterrecovery/
  doa/service/DataAggregationService.java
  doa/service/LLMIntegrationService.java
  doa/service/ReportGenerationService.java
  entities/GeneratedSummary.java
  repository/GeneratedSummaryRepository.java
  exception/{DataAggregationException,LLMServiceException,ReportGenerationException}.java
```

These classes depend on DTOs and entities (`DisasterDataDTO`, `LLMResponseDTO`, `DisasterReport`, `ReportDisasterRepository`) defined elsewhere in the team codebase, so this repository is a reference for the design and implementation rather than a standalone build.
