---
type: integration
title: SonarQube API integration
description: How sqgraph calls SonarQube or SonarCloud, covering the Basic auth header, token validation, project search expansion, paged measures history, the per-page sleep, the response DTOs, and per-project error handling.
tags: [sonarqube, sonarcloud, http, rest-template, paging, rate-limiting]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-05T02:41:47.998Z
sources:
  - id: openwiki-source-9242e856db86f0926c378307
    resource: repo://src/main/java/com/ldiamond/sqgraph/ApiProjectsSearchResults.java
  - id: openwiki-source-65f625fd21310afcb7d1f28b
    resource: repo://src/main/java/com/ldiamond/sqgraph/SqgraphApplication.java
generated: { by: "claude-code", at: "2026-11-05T02:41:47.998Z" }
---

# SonarQube API integration

sqgraph uses three read-only SonarQube Web API endpoints through a single Spring
`RestTemplate` created once per run in `commandLineRunner`. Every URL is built by
appending a path and query string to the config's `url`.

## Authentication

`buildAuthHeaders` sends the token as HTTP Basic auth, Base64-encoding
`token + ":"` (the token as the username with an empty password). The same
`HttpHeaders` object is used for every request.

## Endpoints

| Step | Endpoint | Response DTO | Purpose |
|---|---|---|---|
| Token check | `GET /api/authentication/validate` | `ValidationResult` (`valid`) | Stops the run unless `valid` is true |
| Expansion | `GET /api/projects/search?qualifiers=TRK&q=<query>` | `ApiProjectsSearchResults` | Turns a `query` entry into projects |
| History | `GET /api/measures/search_history?from=<date>&p=<page>&ps=999&component=<key>&metrics=<list>` | `SearchHistory` | Metric history for one project |

### Token validation

`validateSonarToken` returns false if the request throws (for example the server
is unreachable) or the body is empty. Either way the run stops after logging
`Invalid SonarQube token.`, so an unreachable server is reported as a bad token.

### Project search expansion

For each `query` entry, `expandApplications` makes one search request and adds
every returned component as an `Application` with the component's `key`, and its
`name` as the title. Only the first page of search results is read, since the
code doesn't look at paging for this call. Exceptions here aren't caught, so a
failed search request fails the whole run.

### Measures history and paging

`fetchRawMetricsForApps` calls `getHistory` once per expanded project:

- `from` is today in UTC minus `maxReportHistory` days, as `yyyy-MM-dd`.
- `metrics` is the comma-joined set from `getMetricsListNeeded`, the same for
  every project (see [Synthetic metrics](../concepts/synthetic-metrics.md)).

`getHistory` requests pages starting at `p=1` with page size 999, and appends each
page's `measures` to an `AssembledSearchHistory`. It stops when:

- the response body is null, returning what it has so far;
- the response has no `paging` object, which is treated as the last page; or
- `paging.total <= page`.

`paging.total` is a count of items, but it's compared with the page number, so a
project can get roughly one request per item instead of one per 999 items.
Because every page's measures are appended, the same metric can appear in several
`Measures` entries, and chart code iterates all of them.

## Rate limiting

There is no real rate limiter. `getHistory` sleeps 1 ms after every page,
including the last, and `fetchRawMetricsForApps` sleeps another 1 ms after each
project. Nothing handles HTTP 429 or `Retry-After`.

## Error handling

A per-project exception, including any HTTP error status `RestTemplate` throws,
is caught in `fetchRawMetricsForApps` and logged as
`Skipping project '<key>' ...`. That project is then missing from the charts.
An `InterruptedException` re-sets the interrupt flag and returns the projects
fetched so far.

## Response DTOs

All DTOs are Lombok `@Data` classes:

- `SearchHistory` holds `paging` (`Paging`: `pageIndex`, `pageSize`, `total`)
  and `measures` (`Measures[]`).
- `Measures` holds a `metric` key and `history` (`History[]`).
- `History` holds a `date` (`java.util.Date`) and a primitive `double value`.
- `ApiProjectsSearchResults` holds `components`, each with `key` and `name`.
- `AssembledSearchHistory` holds the merged `List<Measures>` across pages. It is
  null when no page returned any measures.

## Related pages

- [Run pipeline and lifecycle](../architecture/run-pipeline.md)
- [Synthetic metrics](../concepts/synthetic-metrics.md)
