---
type: component
title: PDF report
description: How PDFOutput builds the optional single-page OpenPDF report with a header line, a dashboard table of each project's latest values coloured by thresholds, and the chart images with descriptions, plus its error handling and known gaps.
tags: [pdf, openpdf, dashboard, thresholds, report]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-05T02:41:47.998Z
sources:
  - id: openwiki-source-248971930ba84eab9b23662b
    resource: repo://src/main/java/com/ldiamond/sqgraph/PDFOutput.java
  - id: openwiki-source-65f625fd21310afcb7d1f28b
    resource: repo://src/main/java/com/ldiamond/sqgraph/SqgraphApplication.java
  - id: openwiki-source-71dbcbd6716ac5b34c2deeab
    resource: repo://src/test/java/com/ldiamond/sqgraph/PDFOutputTests.java
generated: { by: "claude-code", at: "2026-11-05T02:41:47.998Z" }
---

# PDF report

When the config sets `pdf`, `createPdfIfNeeded` builds a report with `PDFOutput`
after all charts are written, in this order: `createPDF`, `addTextDashboard`,
`addGraphs`, `closePDF`.

## Layout

The report is **one tall page**, 1800 points wide and 1300 points tall for each
configured metric. It contains:

1. A centred line `Created by Management Code Metrics - CodeQualityGraph.com - <time>`,
   with the time in the system time zone as `dd MMM yyyy hh:mm:ss a`. The
   document title is set to `Management Code Metrics`.
2. The **dashboard table**.
3. For each metric with a `filename`, its optional `description` paragraph
   followed by the chart PNG.

## Dashboard table

`addTextDashboard` builds a `PdfPTable` with one column for the project name plus
one per metric, 95% of the page width:

- **Header** (`addHeader`): `Project`, then each metric's `title`, in 20-point
  text on light grey.
- **Rows** (`addTextDashboardBody`): one row per entry in **`config.applications`**
  (the configured list, not the expanded one). Each cell looks up
  `dashboardData(metric title, application title)`, using the same
  `Management Code Metrics` default title as the charts. A missing value shows
  `N/A`. Others use the default-locale `DecimalFormat` `###,###,###.###`.
- **Column widths**: `getWidthOfString` estimates text width (1 unit for narrow
  characters such as `i`, `l`, `t`, `.` and `,`, 3 for capitals, 2 otherwise),
  and `setMax` keeps the widest value per column. The results become relative
  widths through `setWidths`.

Because rows come from the configured list, a `query` entry gives one row with a
blank title and `N/A` values, and the projects it found aren't listed.

### Threshold colouring

A cell with a value is coloured only when the metric has both `green` and
`yellow`. The thresholds are parsed as doubles, and the comparison uses the
numeric value, not the formatted text:

| Threshold order | Green | Yellow | Pink |
|---|---|---|---|
| `green >= yellow` (higher is better) | value > green | value > yellow | otherwise |
| `yellow > green` (lower is better) | value < green | value < yellow | otherwise |

Comparisons are strict, so a value exactly equal to a threshold gets the next,
worse colour.

## Error handling

- `addTextDashboard` catches any exception, prints it, and leaves the table out.
  The rest of the PDF is still written.
- `addGraphs` wraps the **whole loop** in one `try` that catches `IOException`. If
  one chart image is missing (for example because that chart failed), that image
  and every later one are left out.
- `createPDF` catches `IOException` from opening the output file and returns the
  document anyway. Later steps then work on a document that was never opened.

None of these failures change the exit code, see
[Run pipeline and lifecycle](../architecture/run-pipeline.md).

## Tests

`PDFOutputTests` covers width estimation, `setMax`, every colouring branch,
header construction, dashboard rows (including a `6,002.5` value that used to
break colouring, and missing data showing `N/A`), and `closePDF`. Tests check cell
contents and colours on `PdfPTable` objects rather than rendering PDFs. The
formatting calls (font size, padding, alignment) aren't asserted, so most
surviving mutants are in this class, see
[Tests and mutation testing](../testing/test-suite.md).

## Related pages

- [Chart rendering](charts.md)
- [Configuration file and credentials](../configuration/config-file.md)
