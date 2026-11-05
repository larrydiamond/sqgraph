# Files

- [Chart rendering](charts.md) - How GraphOutput turns fetched SonarQube history into one XChart PNG per configured metric, covering legend layout, date normalization, filename defaults, the dashboard values it records for the PDF, and how failures are isolated per metric.
- [PDF report](pdf-report.md) - How PDFOutput builds the optional single-page OpenPDF report with a header line, a dashboard table of each project's latest values coloured by thresholds, and the chart images with descriptions, plus its error handling and known gaps.
