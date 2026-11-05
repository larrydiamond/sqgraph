---
type: architecture
title: Run pipeline and lifecycle
description: How one sqgraph run flows from main() through Spring Boot startup and the commandLineRunner bean to charts and the optional PDF, including ordering, shared static state, and exit-code behavior on failure.
tags: [architecture, lifecycle, spring-boot, error-handling]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-05T02:41:47.998Z
sources:
  - id: openwiki-source-65f625fd21310afcb7d1f28b
    resource: repo://src/main/java/com/ldiamond/sqgraph/SqgraphApplication.java
generated: { by: "claude-code", at: "2026-11-05T02:41:47.998Z" }
---

# Run pipeline and lifecycle

sqgraph is a single-shot command-line program. It reads one JSON config file, pulls
metric history from SonarQube or SonarCloud, writes one PNG chart per configured
metric, and optionally writes a PDF report. All orchestration lives in
`SqgraphApplication`.

## Entry point: `main()`

`main()` runs before Spring starts and does two checks:

1. **Token.** `getSonarQubeToken()` looks up the environment variables
   `SONARLOGIN`, `SONAR_TOKEN`, `SONAR_LOGIN` and `SONARTOKEN`, in that order, and
   uses the first one set. If none is set it logs an error and calls
   `System.exit(1)`.
2. **Config path.** If there is no first argument, it logs an error and exits 1.

The token and config path are stored in the **static fields** `login` and
`filename`, and then `SpringApplication.run` starts the context. The Spring bean
reads those statics. This is the only channel between `main()` and the pipeline, so
code that builds the context without going through `main()` (for example a test)
sees `null` for both.

`application.properties` only turns off the Spring banner. No web server is
configured, so the context starts, runs the pipeline, and exits.

## The pipeline runs during bean creation

The real work happens inside the `@Bean` method `commandLineRunner(...)`, **while
Spring is creating the bean**. The `CommandLineRunner` it returns is an empty
lambda (`args -> {}`). As a result:

- The whole report is produced during context refresh, not in the runner callback.
- An exception that escapes the bean method fails context startup. Spring logs a
  `BeanCreationException`, and the process exits with a non-zero code.

The steps, in order:

| Step | Method | What it does |
|---|---|---|
| 1 | `loadConfig` | Jackson reads the JSON file into `Config`. On `IOException` it prints the stack trace and returns `null`. |
| 2 | `populateSynthetics` | Builds the map of synthetic (derived) metrics, see [Synthetic metrics](../concepts/synthetic-metrics.md). |
| 3 | `buildAuthHeaders` | Creates a Basic auth header from the token. |
| 4 | `validateSonarToken` | Calls `/api/authentication/validate`, see [SonarQube API integration](../integrations/sonarqube-api.md). |
| 5 | `expandApplications` | Turns `query` entries into concrete projects and fills `Config.expandedApplications`. |
| 6 | `buildTitleLookup` | Maps project key to display title. |
| 7 | `fetchRawMetricsForApps` | Fetches history per expanded project. |
| 8 | `GraphOutput.outputGraphs` | Writes one PNG per metric and fills the dashboard table, see [Chart rendering](../outputs/charts.md). |
| 9 | `createPdfIfNeeded` | Writes the PDF only when `pdf` is set, see [PDF report](../outputs/pdf-report.md). |

Steps 5–9 are grouped in `loadDataAndGenerateReport`, which tests call directly
with a mocked `RestTemplate`.

## Data handed between steps

- `Config.expandedApplications` is a `transient` list that step 5 overwrites on
  every run. All later steps iterate the expanded list. The PDF dashboard is the
  exception: it iterates the original `applications` array (see
  [PDF report](../outputs/pdf-report.md)).
- `rawMetrics` is a `Map<projectKey, AssembledSearchHistory>`. A project whose
  fetch fails is simply absent from it.
- `dashboardData` is a Guava `HashBasedTable<metricTitle, applicationTitle,
  lastValue>`. Chart rendering fills it and the PDF reads it.
- `SQMetrics.filename` is changed in place by chart rendering (defaulted and given
  a `.png` suffix), and the PDF then uses it to find the image files.

## Failure and exit-code behavior

The pipeline mostly logs and keeps going, so the exit code says little about
whether the report is complete:

| Situation | Behavior | Exit code |
|---|---|---|
| No token variable set, or no config argument | Error logged in `main()` | 1 |
| Config file missing or unreadable | Error logged, bean returns an empty runner | 0 |
| Token rejected, or server unreachable during validation | `Invalid SonarQube token.` logged, empty runner | 0 |
| One project's history fetch throws | Project skipped with `Skipping project ...` | 0 |
| One metric's chart throws | Stack trace printed, other metrics continue | 0 |
| An exception escapes the bean method (for example during application expansion) | Context startup fails | non-zero |

`Successful completion.` is logged whenever steps 5–9 return. It doesn't mean
every project and chart succeeded.

## Related pages

- [Configuration file and credentials](../configuration/config-file.md)
- [SonarQube API integration](../integrations/sonarqube-api.md)
- [Chart rendering](../outputs/charts.md)
- [PDF report](../outputs/pdf-report.md)
