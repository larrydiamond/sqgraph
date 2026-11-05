---
type: testing
title: Tests and mutation testing
description: How sqgraph is tested, covering JUnit and Mockito unit tests with a mocked RestTemplate, ArchUnit rules, JaCoCo coverage, PIT mutation testing setup and caveats, files tests leave behind, and how to run focused tests.
tags: [testing, junit, mockito, archunit, jacoco, pitest, mutation-testing]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-05T02:41:47.998Z
sources:
  - id: openwiki-source-18fdeaac7d12c7b7f20e3a74
    resource: repo://build.gradle
  - id: openwiki-source-65f625fd21310afcb7d1f28b
    resource: repo://src/main/java/com/ldiamond/sqgraph/SqgraphApplication.java
  - id: openwiki-source-f89684a47f3e04483e8183bd
    resource: repo://src/test/java/com/ldiamond/sqgraph/ArchitectureUnitTests.java
  - id: openwiki-source-a05a9cf778bf1993b976d6db
    resource: repo://src/test/java/com/ldiamond/sqgraph/SqgraphApplicationTests.java
generated: { by: "claude-code", at: "2026-11-05T02:41:47.998Z" }
---

# Tests and mutation testing

All tests are plain unit tests on the JUnit Platform (JUnit 6 through the Spring
Boot test starter). None of them start a Spring context or reach a real
SonarQube server.

## Test classes

| Class | Focus |
|---|---|
| `SqgraphApplicationTests` | Token lookup, auth header, token validation, application expansion, title lookup, history paging, per-project error isolation, synthetic metric formulas, and full `loadDataAndGenerateReport` runs |
| `GraphOutputTests` | Native and synthetic series building, and rendering a real PNG |
| `PDFOutputTests` | Dashboard table cells and colours, width estimation, header, `closePDF` |
| `ArchitectureUnitTests` | Architecture rules |

### How tests isolate the network

`SqgraphApplicationTests` uses `MockitoExtension`. HTTP calls are stubbed on a
mocked `RestTemplate` by matching `exchange(...)` on the response class
(`SearchHistory`, `ApiProjectsSearchResults` or `ValidationResult`). Pipeline
methods that tests need to reach are package-visible and marked
`@VisibleForTesting`. Examples:

- `getSonarQubeToken(UnaryOperator<String>)` takes an injected environment
  lookup.
- `testGetHistory_missingPagingStopsAfterFirstPage` stubs a second response that
  throws `AssertionError`, so a paging regression fails at once instead of
  looping forever.
- PDF output goes to a JUnit `@TempDir`.

### Files tests write

Tests that call `loadDataAndGenerateReport` with metrics that have no title or
filename write `Management Code Metrics.png` into the **current working
directory**, normally the repository root. That file isn't ignored by git, and a
same-named file there gets overwritten.

### Architecture rules

`ArchitectureUnitTests` uses `com.ldiamond:architectureunittests` and ArchUnit:

- no class may use the standard streams (`System.out` and `System.err`);
- no public static field may be a `SimpleDateFormat`, because it isn't
  thread-safe;
- methods annotated with `@Async` must be public.

## Running tests

```
./gradlew test
./gradlew test --tests 'com.ldiamond.sqgraph.PDFOutputTests'
./gradlew test --tests '*testGetHistory_missingPagingStopsAfterFirstPage'
```

Each test run also writes JaCoCo XML and CSV reports, which SonarCloud reads in
CI, see [Build, CI and code quality tooling](../operations/build-and-ci.md).

## Mutation testing with PIT

`./gradlew pitest` runs PIT on every class in `com.ldiamond.sqgraph`, using the
`*Tests` classes except `ArchitectureUnitTests`, and writes HTML and XML reports
to `build/reports/pitest/`. It isn't part of `build` or CI, and there's no
minimum score. Three settings are needed for correct results:

| Setting | Why |
|---|---|
| `addJUnitPlatformLauncher = false` | The plugin otherwise adds a 1.x JUnit launcher that clashes with JUnit 6, and every test looks like it failed |
| `threads = 1` | Tests write the same chart filenames, so parallel workers collide and report false kills that change between runs |
| `workingDir = build/pitest-work` | Mutants can change where charts are written, which keeps those files out of the repository |

Most surviving mutants are formatting calls in `PDFOutput` (font size, padding,
alignment), along with logging and `printStackTrace` lines. The survivors that
matter more are in synthetic value combination and in report generation steps
that no test asserts.

## Related pages

- [Build, CI and code quality tooling](../operations/build-and-ci.md)
- [Run pipeline and lifecycle](../architecture/run-pipeline.md)
