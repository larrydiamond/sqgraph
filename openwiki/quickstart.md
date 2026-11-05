---
type: quickstart
title: Quickstart
description: Orientation for sqgraph (Management Code Metrics), a Java 21 command-line tool that charts SonarQube or SonarCloud metric history across projects, with how to build and run it and a map of which wiki page answers which question.
tags: [quickstart, overview, navigation]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-05T02:41:47.998Z
sources:
  - id: openwiki-source-23775c3de52f3ab95a13cb8b
    resource: repo://README.md
  - id: openwiki-source-56d49f2ca6d2a4b00ed10210
    resource: repo://src/main/java/com/ldiamond/sqgraph/GraphOutput.java
  - id: openwiki-source-65f625fd21310afcb7d1f28b
    resource: repo://src/main/java/com/ldiamond/sqgraph/SqgraphApplication.java
generated: { by: "claude-code", at: "2026-11-05T02:41:47.998Z" }
---

# Quickstart

**sqgraph** ("Management Code Metrics") compares code-quality trends across many
SonarQube or SonarCloud projects. Given a JSON config listing projects and
metrics, it fetches each project's metric history, writes **one PNG line chart
per metric** with a line per project, and can also write a **PDF report** with a
colour-coded dashboard table of each project's latest values.

It is a single Spring Boot command-line application in one package,
`com.ldiamond.sqgraph`, with about ten classes. There's no server, database or
persistent state. Each run reads the config, calls the SonarQube Web API, and
writes files to the working directory.

## Build and run

```
./gradlew build
export SONAR_TOKEN=<your user token>
java -jar build/libs/sqgraph-<version>.jar samples/sonarcloud/sonarcloud.json
```

The token can also be given as `SONARLOGIN`, `SONAR_LOGIN` or `SONARTOKEN`.
Without a token or a config argument the program exits with status 1. Other
failures are logged but still exit 0, so check the log and the output files.

## Main classes

| Class | Role |
|---|---|
| `SqgraphApplication` | Entry point, pipeline orchestration, SonarQube calls, synthetic metric definitions |
| `Config`, `Application`, `SQMetrics` | The JSON config model |
| `SearchHistory`, `Measures`, `ApiProjectsSearchResults`, `ValidationResult`, `AssembledSearchHistory` | API response DTOs |
| `SyntheticMetric` | Interface for derived metrics |
| `GraphOutput` | PNG charts and the dashboard values |
| `PDFOutput` | The PDF report |

## Where to look

| If you want to… | Read |
|---|---|
| Understand what happens during a run, and why it exits 0 after a failure | [Run pipeline and lifecycle](architecture/run-pipeline.md) |
| Write or debug a config file, or supply the token | [Configuration file and credentials](configuration/config-file.md) |
| Add or debug a derived metric such as `ViolationsPerKLines` or `x__PER_K_y` | [Synthetic metrics](concepts/synthetic-metrics.md) |
| Change API calls, paging, rate limiting, or HTTP error handling | [SonarQube API integration](integrations/sonarqube-api.md) |
| Change chart appearance, filenames, or what feeds the dashboard | [Chart rendering](outputs/charts.md) |
| Change the PDF table, threshold colours, or image layout | [PDF report](outputs/pdf-report.md) |
| Run or write tests, or use mutation testing | [Tests and mutation testing](testing/test-suite.md) |
| Change the build, CI, SonarCloud analysis, or dependencies, or check contribution rules | [Build, CI and code quality tooling](operations/build-and-ci.md) |

## Things that surprise newcomers

- The whole report is generated while Spring creates a bean, not in a
  `CommandLineRunner` callback.
- Most failures are logged and swallowed, and `Successful completion.` doesn't
  mean every chart was produced.
- Charts and tests write to the current working directory. Tests can overwrite
  `Management Code Metrics.png` in the repository root.
- Work happens on feature branches only, and `main` is protected by the
  conventions in `AGENTS.md`.
