---
type: component
title: Chart rendering
description: How GraphOutput turns fetched SonarQube history into one XChart PNG per configured metric, covering legend layout, date normalization, filename defaults, the dashboard values it records for the PDF, and how failures are isolated per metric.
tags: [charts, xchart, png, graph-output, dashboard]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-05T02:41:47.998Z
sources:
  - id: openwiki-source-56d49f2ca6d2a4b00ed10210
    resource: repo://src/main/java/com/ldiamond/sqgraph/GraphOutput.java
  - id: openwiki-source-d6ad539237ee433a1471282f
    resource: repo://src/test/java/com/ldiamond/sqgraph/GraphOutputTests.java
generated: { by: "claude-code", at: "2026-11-05T02:41:47.998Z" }
---

# Chart rendering

`GraphOutput.outputGraphs` is called once per run, after all history has been
fetched. It writes one PNG per entry in `config.metrics`, with one line (series)
per project, and fills the `dashboardData` table that the
[PDF report](pdf-report.md) reads.

## Per-metric flow

For each `SQMetrics` entry:

1. **Title.** It uses the metric's `title`, or `Management Code Metrics` if none
   is set.
2. **Chart.** It builds a 1650×800 `XYChart`. With **more than 7 projects** the
   legend goes outside on the right in a vertical list, otherwise outside at the
   bottom in a horizontal row. Axis titles are hidden, dates are formatted
   `dd MMM yyyy`, and the y-axis uses `###,###,###.###`.
3. **Series.** For each project in `rawMetrics`, `addSeriesForMetric` builds the
   project's points and adds a series named with the project's display title (from
   the title lookup).
4. **File.** If `filename` is missing it is set to the title, and `.png` is
   appended if needed. This **changes the `SQMetrics` object**, and the PDF later
   reads the final name. The chart is saved with XChart's `ChartEncoder`, relative
   to the working directory.

The whole per-metric body is wrapped in `catch (Exception)`. A failure prints a
stack trace and moves on to the next metric, so one bad metric doesn't stop the
others. A failure before step 4, though, leaves `filename` unset or unsuffixed,
and the PDF then skips or fails to find that image.

## Building a project's points

`addSeriesForMetric` picks a path based on whether the metric name is a synthetic
metric:

- **Native metric** (`addSeriesForNativeMetric`): adds every history point of
  every `Measures` entry whose `metric` equals the name. Because paged results
  are appended, this can include several entries for the same metric.
- **Synthetic metric** (`addSeriesForSyntheticMetric`): calculates values per
  date, see [Synthetic metrics](../concepts/synthetic-metrics.md).

A series is added only if it has at least one point. Either way, the project's
**last value** goes into `dashboardData` under `(metric title, project title)`.
For a native metric with no matching data that value is 0.0, not missing, so the
PDF shows `0` rather than `N/A`.

### Date normalization

Every point's date is moved to **midnight UTC of the following day**
(`getUTCDate` truncates to the day in UTC and adds one day). So several analyses
on the same day plot at the same x position.

## Failure modes worth knowing

- A project whose `AssembledSearchHistory.measures` is null (no page returned
  measures), or a measure with a null `history`, throws a
  `NullPointerException` inside the per-metric `try`. That loses the whole chart
  for every project, not just the one with missing data.
- Guava's `HashBasedTable.put` rejects null keys, so a project without a display
  title would fail at the dashboard step. `Application.getTitle()` falls back to
  the key to avoid that.
- XChart rejects duplicate series names, so two projects with the same title
  can't share a chart.

## Tests

`GraphOutputTests` covers the native and synthetic series builders with mocked
charts and hand-built histories. `testOutputGraphs` renders a real PNG to a temp
file and checks the dashboard value. Tests that build configs without filenames
write `Management Code Metrics.png` into the working directory, see
[Tests and mutation testing](../testing/test-suite.md).

## Related pages

- [Synthetic metrics](../concepts/synthetic-metrics.md)
- [PDF report](pdf-report.md)
- [Run pipeline and lifecycle](../architecture/run-pipeline.md)
