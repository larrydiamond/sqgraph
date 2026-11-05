---
type: configuration
title: Configuration file and credentials
description: The JSON config file that drives a sqgraph run (Config, Application and SQMetrics), how it is parsed, which defaults apply, how the SonarQube token is supplied, and the sample configs.
tags: [configuration, json, credentials, samples]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-05T02:41:47.998Z
sources:
  - id: openwiki-source-1b70553092cd130bba090af0
    resource: repo://samples/sonarcloud/sonarcloud.json
  - id: openwiki-source-317817ad3a14c64d3d1a767a
    resource: repo://src/main/java/com/ldiamond/sqgraph/Config.java
  - id: openwiki-source-56d49f2ca6d2a4b00ed10210
    resource: repo://src/main/java/com/ldiamond/sqgraph/GraphOutput.java
  - id: openwiki-source-65f625fd21310afcb7d1f28b
    resource: repo://src/main/java/com/ldiamond/sqgraph/SqgraphApplication.java
generated: { by: "claude-code", at: "2026-11-05T02:41:47.998Z" }
---

# Configuration file and credentials

A run is driven by one JSON file, passed as the first command-line argument, plus
a SonarQube user token from the environment.

## Credentials

The token never goes in the config file. `main()` reads the first set variable
from this list, in order:

1. `SONARLOGIN`
2. `SONAR_TOKEN`
3. `SONAR_LOGIN`
4. `SONARTOKEN`

If none is set the program exits with status 1. The token is sent as HTTP Basic
auth with an empty password, see
[SonarQube API integration](../integrations/sonarqube-api.md).

## Parsing

`loadConfig` uses a plain Jackson `ObjectMapper` with default settings to read the
file into `Config`. If parsing fails (missing file, invalid JSON, wrong types),
the stack trace is printed, `Unable to load config file` is logged, and the run
ends without producing anything. That still exits with status 0, see
[Run pipeline and lifecycle](../architecture/run-pipeline.md).

The model classes are Lombok `@Data` classes in `Config.java`, and all three are
`Serializable`.

## `Config` (top level)

| Field | Type | Meaning |
|---|---|---|
| `url` | string | Base URL of the SonarQube server, or `https://sonarcloud.io`. Request paths are appended directly, so it shouldn't end with `/`. |
| `applications` | array | Projects to chart, see below. |
| `metrics` | array | Metrics to chart, one PNG each, see below. |
| `maxReportHistory` | int | Days of history to request. Defaults to 0. The samples pass it as a string (`"366"`), which Jackson converts. |
| `pdf` | string | Optional. If set, the PDF report is written to this path. |
| `dashboard` | string | Present in the model and in both samples, but no code reads it. |
| `expandedApplications` | list | Runtime only (`transient`). Overwritten on each run by application expansion. |

## `applications[]`: `Application`

Each entry is either a single project or a search:

- **`key`**, optionally with **`title`**: one SonarQube project. `getTitle()`
  returns the title, or the key when the title is missing or blank. That title is
  used in chart legends and the PDF.
- **`query`**: with no `key`, sqgraph searches `/api/projects/search` for matching
  projects and charts each one, using SonarQube's project name as the title.

An entry with neither `key` nor `query` is ignored.

## `metrics[]`: `SQMetrics`

| Field | Meaning |
|---|---|
| `metric` | A SonarQube metric key, a built-in synthetic metric, or a `__PER__` ratio, see [Synthetic metrics](../concepts/synthetic-metrics.md). |
| `title` | Chart title and PDF dashboard column. Defaults to `Management Code Metrics` when missing. |
| `filename` | Output PNG path. Defaults to the title, and `.png` is appended if missing. Relative paths are relative to the working directory. |
| `green`, `yellow` | Optional thresholds for colouring the PDF dashboard, as strings parsed to numbers. See [PDF report](../outputs/pdf-report.md). |
| `description` | Optional text printed above the chart in the PDF. |

Two metrics without titles or filenames both default to
`Management Code Metrics.png`, so the second overwrites the first.

## Samples

- `samples/sonarcloud/sonarcloud.json` charts six public SonarCloud projects
  with coverage and three synthetic metrics, and writes `CodeQuality.pdf`. The
  PNGs and PDF next to it are its example output.
- `samples/selfhosted/sample.json` targets a local server at
  `http://127.0.0.1:9000` and shows a custom ratio
  (`critical_violations__PER_K_ncloc`).

## Related pages

- [Synthetic metrics](../concepts/synthetic-metrics.md)
- [Run pipeline and lifecycle](../architecture/run-pipeline.md)
- [PDF report](../outputs/pdf-report.md)
