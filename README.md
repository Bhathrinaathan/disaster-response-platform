# Response to City Disasters: an LLM-assisted Disaster Management Platform

A multi-user platform for reporting, validating and responding to urban disasters, built by a ten-person team using Extreme Programming over two semesters of the Advanced Software Engineering module, MSc Computer Science, Trinity College Dublin (2025–26).

This repository holds **my contribution to the backend** — the automated disaster-summary subsystem — plus an overview of the full system. The complete team codebase lives in the module's private GitLab and is not reproduced here.

## The platform

```
Civilians ─ report incidents (form or voice), view shelters & evacuation routes
                 │
                 ▼
Spring Boot API ─ validation engine: checks each report against weather, traffic, places,
                 │  social-media and public-transport sources → INVALID / PARTIALLY_VALIDATED
                 │  with a confidence score; an admin promotes to VALIDATED
                 │  ├─ SQS ingestion of incoming reports, S3 for media
                 │  ├─ Emergency Response Team (ERT) resource allocation + HERE Maps routing
                 │  └─ LLM-backed summary & report generation  ◀── this repo
                 ▼
Authorities ─ preview, adjust and dispatch units across Garda / Fire Brigade / HSE / Council,
              publish civilian-facing summaries
```

**Stack:** Java 24, Spring Boot 3.5.7 (Gradle); React 19 + TypeScript (Vite); PostgreSQL on AWS RDS; backend in Docker on ECS behind an Application Load Balancer; S3 for media, SQS for ingestion, SSM Parameter Store for secrets; frontend via CloudFront; JWT authentication; HERE Maps for routing; Web Push and SSE for notifications. Tests: JUnit 5 and Mockito on the backend, Vitest on the frontend. CI/CD: GitLab CI/CD.

**Process:** Extreme Programming across two semesters and six two-week sprints: five driver–navigator pairs per sprint, pairs reshuffled each sprint, review inside the pair and then by a second pair before merge, and a rotating Scrum Master.

## What I built

### 1. Resource allocation for Emergency Response Teams
When an authority user clicks *Calculate ERT*, the service recommends units per agency from the disaster's type, severity and scale; the user previews, adjusts and dispatches, and HERE Maps routes are computed and stored, with re-routing re-querying HERE Maps against live traffic. Allocation prefers the nearest available units and substitutes across departments when one agency is exhausted. (Lives in the team repository; summarised here because the summary service below consumes its output.)

### 2. LLM-backed disaster-summary pipeline (code in `src/`)

| Class | Responsibility |
|---|---|
| `DataAggregationService` | Collects everything known about a report: the database record (required), social-media signals, weather and external feeds, and current resource allocations (each optional). A failing optional source is logged and skipped so a summary can still be produced; a failing primary source raises `DataAggregationException`. |
| `LLMIntegrationService` | Builds a structured prompt from the aggregated data, calls the configured chat-completion endpoint (Grok, via the xAI API, in the deployed system) with bounded retries, and parses the response into two audiences: an **authority summary** (operational detail, resource status) and a **civilian summary** (plain-language guidance). Failures surface as `LLMServiceException`. |
| `ReportGenerationService` | Renders both summaries to self-contained HTML documents (openable directly in Word, no extra dependencies), writes them to the configured output directory and persists a `GeneratedSummary` row in `DRAFT` state for an authority to review and publish. |
| `GeneratedSummary` / `GeneratedSummaryRepository` | JPA entity and Spring Data repository for generated summaries, their document paths, author and publication state. |
| `*Exception` | Typed exceptions so the controller layer can map aggregation, LLM and report failures to distinct HTTP responses. |

Configuration is externalised through `application.properties` (`llm.api.url`, `llm.model`, `llm.max.retries`, `report.output.directory`, external API URLs), so the same code runs against a mock endpoint in tests and a real provider in deployment.

### 3. AI rerouting tests and cross-service integration
Wrote the test cases for the AI-assisted evacuation rerouting feature and the integration tests that exercise the report → validation → allocation → summary path end to end. In semester 1 I also gathered and integrated external APIs for the thin-slice build, and I demoed the platform live.

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
