---
type: operations
title: Build, CI and code quality tooling
description: How sqgraph is built with Gradle into a runnable Spring Boot jar, which libraries it uses, what GitHub Actions runs (SonarCloud analysis and the scheduled OpenWiki update), and the dependency, refactoring and contribution tooling.
tags: [build, gradle, ci, github-actions, sonarcloud, openrewrite, dependencies]
verified:
  - by: openwiki/0.8.0
    at: 2026-11-05T02:41:47.998Z
sources:
  - id: openwiki-source-7a80b79a6fb3618cbfab08a2
    resource: repo://.github/workflows/build.yml
  - id: openwiki-source-6d4b4e707b8d60b6ccfa3425
    resource: repo://.github/workflows/openwiki-update.yml
  - id: openwiki-source-8037e2358a2c4f9b2c722a11
    resource: repo://AGENTS.md
  - id: openwiki-source-18fdeaac7d12c7b7f20e3a74
    resource: repo://build.gradle
generated: { by: "claude-code", at: "2026-11-05T02:41:47.998Z" }
---

# Build, CI and code quality tooling

## Build

The project is a single Gradle module (`rootProject.name = 'sqgraph'`) built with
the wrapper (`./gradlew`, never a system `gradle`). It targets **Java 21**
(`sourceCompatibility` and `targetCompatibility`).

| Command | Result |
|---|---|
| `./gradlew build` | Compiles, runs tests and JaCoCo, and builds `build/libs/sqgraph-<version>.jar` (the runnable Spring Boot jar) plus a `-plain` jar |
| `./gradlew test` | Unit tests. JaCoCo XML and CSV reports always follow |
| `./gradlew pitest` | Mutation testing. It isn't part of `build`, see [Tests and mutation testing](../testing/test-suite.md) |
| `./gradlew rewriteRun` | Applies the OpenRewrite recipes |
| `./gradlew dependencyCheckAnalyze` | OWASP dependency check. Reads the NVD API key from `NVD_KEY` |
| `./gradlew dependencyUpdates` / `useLatestVersions` | Reports or applies newer dependency versions |

Run the jar with `java -jar build/libs/sqgraph-<version>.jar <config.json>` and a
token in the environment, see
[Configuration file and credentials](../configuration/config-file.md).

## Main libraries

| Library | Used for |
|---|---|
| Spring Boot 4 starter and `spring-web` | Application lifecycle and `RestTemplate` |
| Jackson 2 (`jackson-databind`) | Config file and API responses |
| XChart 4 | PNG charts |
| OpenPDF 3 | PDF report |
| Guava | `HashBasedTable` for dashboard data, `@VisibleForTesting` |
| Apache Commons Lang 3 | String helpers |
| Lombok (compile time only) | `@Data`, `@Slf4j`, `@NoArgsConstructor` |

`commons-logging` is excluded from several dependencies and replaced by the
`jcl-over-slf4j` bridge. Spring milestone and snapshot repositories are
configured alongside Maven Central. Several test dependencies use
`latest.release`, so test builds aren't fully reproducible over time.

## Continuous integration

`.github/workflows/build.yml` (named "SonarQube") runs on pushes to `main` and on
pull requests. It checks out with full history, sets up Zulu JDK 21 with the
Gradle cache, and runs `./gradlew build test sonar --info` with the
`SONAR_TOKEN` secret. SonarCloud settings (`larrydiamond_sqgraph`, organization
`larrydiamond-github`) are in `build.gradle`. Analysis covers only
`src/main/java/com/ldiamond/sqgraph`, reads coverage from the JaCoCo XML report,
and excludes the plain data classes (`Config`, `SearchHistory`,
`ValidationResult`, `MetricsResults`) from coverage. Mutation testing doesn't run
in CI.

`.github/workflows/openwiki-update.yml` runs daily at 08:00 UTC and on manual
dispatch. It installs OpenWiki, runs `openwiki code --update`, and opens or
updates a pull request on the `openwiki/update` branch with the `openwiki/`
folder, `AGENTS.md` and the workflow file. If the OpenWiki step fails, the PR
still keeps the pages finished before the failure, and the job then fails.

## OpenRewrite

The `rewrite` block activates a curated list of recipes: test assertion
clean-ups, static-analysis fixes, Java and Guava migrations, Lombok
getter/setter conversion, Spring Boot 3.5 upgrade steps, and JUnit 5 to 6
migration. Run `rewriteRun` deliberately and review the diff, because recipes
change source files.

## Contribution rules

`AGENTS.md` sets rules for agents and contributors:

- Never commit or push to `main`; work on feature branches.
- The code must compile and run before committing.
- Keep commits narrowly scoped.
- Don't introduce SonarQube bugs or security violations.
- Don't remove comments or commented-out code unless asked.
- Prefer Lombok, Apache Commons and Spring over custom code, and don't use
  `org.apache.commons.lang3.tuple`.
- Pull requests need human approval, must state that they were AI-created, and
  must list the models used in the PR title.

## Related pages

- [Tests and mutation testing](../testing/test-suite.md)
- [Quickstart](../quickstart.md)
