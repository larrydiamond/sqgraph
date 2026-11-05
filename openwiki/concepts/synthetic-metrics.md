---
type: concept
title: Synthetic metrics
description: How sqgraph derives metrics that SonarQube doesn't provide, such as violations per thousand lines, covering the SyntheticMetric contract, the built-in metrics, the __PER__ naming convention, date alignment, and zero or missing value behavior.
tags: [metrics, synthetic-metrics, calculation, charts]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-05T02:41:47.998Z
sources:
  - id: openwiki-source-56d49f2ca6d2a4b00ed10210
    resource: repo://src/main/java/com/ldiamond/sqgraph/GraphOutput.java
  - id: openwiki-source-65f625fd21310afcb7d1f28b
    resource: repo://src/main/java/com/ldiamond/sqgraph/SqgraphApplication.java
generated: { by: "claude-code", at: "2026-11-05T02:41:47.998Z" }
---

# Synthetic metrics

A **synthetic metric** is a value sqgraph calculates from one or more real
SonarQube metrics, for example violations per thousand lines of code. Any
`metric` in the config that matches a synthetic metric name is calculated
locally. Every other name is sent to SonarQube as a real metric key.

## The contract

`SyntheticMetric` is a three-method interface:

- `getSyntheticName()`: the name used in the config's `metric` field.
- `getRealMetrics()`: the SonarQube metric keys it needs.
- `calculate(Map<String, Double>)`: computes one value from the real values
  for a single date.

`populateSynthetics(config)` builds the lookup map once per run.
`GraphOutput.addSeriesForMetric` checks that map to decide between the native
and synthetic paths.

## Built-in metrics

These four are always registered:

| Name | Real metrics | Formula |
|---|---|---|
| `ViolationsPerKLines` | `violations`, `ncloc` | 1000 × violations / ncloc |
| `CognitiveComplexityPerKLines` | `cognitive_complexity`, `ncloc` | 1000 × complexity / ncloc |
| `BugsPlusSecurity` | `bugs`, `vulnerabilities`, `security_hotspots` | sum of the three |
| `BugsPlusSecurityPerKLines` | the three above plus `ncloc` | 1000 × sum / ncloc |

## Custom ratios from the metric name

`populateSynthetics` also reads each configured metric name and registers a
ratio when it contains one of these separators:

| Separator | Multiplier | Example |
|---|---|---|
| `__PER__` | 1 | `bugs__PER__ncloc` |
| `__PER_K_` | 1000 | `violations__PER_K_ncloc` |
| `__PER_H_` | 100 | `code_smells__PER_H_ncloc` |

The text before the separator is the numerator metric and the text after it is
the denominator. The registered synthetic metric's name is the full configured
string.

## Which metrics are fetched

`getMetricsListNeeded` builds the set of SonarQube metric keys to request. For
each configured metric it adds the name itself if it isn't synthetic, or the
synthetic metric's real metrics if it is. The same set is requested for every
project. See [SonarQube API integration](../integrations/sonarqube-api.md).

## How values are calculated per date

`GraphOutput.addSeriesForSyntheticMetric` aligns values by exact timestamp:

1. It finds the **first** measure in the history that is one of the synthetic
   metric's real metrics. That measure's history dates become the x-axis.
2. For each of those dates, it collects the value of every real metric whose
   history has an entry with the same `Date`, and calls `calculate`.
3. The last calculated value is returned for the dashboard table.

So a date that one metric has but another lacks is calculated with the other
metric missing from the map.

## Zero and missing values

- **Ratio metrics built by `getMetric`** (the two built-in per-KLines metrics
  and all `__PER__` ratios) treat a missing value as 0 and return 0.0 when either
  the numerator or denominator is 0, so they never divide by zero.
- **`BugsPlusSecurity`** treats missing inputs as 0.
- **`BugsPlusSecurityPerKLines`** has **no zero check**. If `ncloc` is 0 or
  missing for a date, it divides by zero and returns infinity or NaN. XChart
  rejects infinity, so that metric's chart fails for the run.

## Extending

To add a built-in metric, create a `SyntheticMetric` constant in
`SqgraphApplication` (or use `getMetric` for a simple ratio) and register it in
`populateSynthetics`. Tests in `SqgraphApplicationTests` call each built-in
metric's `calculate` directly.

## Related pages

- [Chart rendering](../outputs/charts.md)
- [Configuration file and credentials](../configuration/config-file.md)
- [SonarQube API integration](../integrations/sonarqube-api.md)
