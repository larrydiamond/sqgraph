# AGENTS.md

## Setup commands

- Compile application: `./gradlew build`
- Run tests: `./gradlew test`
- Run mutation tests: `./gradlew pitest` (report in `build/reports/pitest/index.html`)
- Run OpenRewrite codegen recipes: `./gradlew rewriteRun`

## Dev environment

- Java 21
- Gradle (use `gradlew`, not `gradle` command)

## Coding conventions

- The use of Lombok is preferred over writing custom code
- Existing classes and methods from Apache Commons or Spring is preferred over writing custom code
- Variable names should be meaningful
- Nothing can be commited or pushed to the main branch
- All changes must occur on feature branches
- Before commiting or pushing code, the code must compile and run successfully
- Agents may create pull requests, at least one authorized human must review and approve all pull requests
- Code commits must be very narrowly scoped, do not change anything outside of the strict narrow scope of the ask
- No commit may create a SonarQube bug or a SonarQube security violation
- Do not remove comments unless explicitly asked to
- Do not remove commented out code unless explicitly asked to
- Do not use the classes in the org.apache.commons.lang3.tuple package

## Pull requests

- Pull requests must state the reason for the change, that the change was created by an AI, and the models used must be listed in the PR title.

<!-- OPENWIKI:START -->

## OpenWiki

This repository has a generated `openwiki/` evidence index. It is optional just-in-time context, not required startup reading.

- Do not enumerate, preload, or search wikis at task start. Use retrieval when the user asks for it, when unfamiliar architecture or dependency behavior materially affects the task, or when source inspection leaves an important uncertainty. Stop once the question is grounded.
- When those conditions apply and OpenWiki retrieval tools are available, use `openwiki_search` for just-in-time context and `openwiki_read` for the relevant complete sections. If search returns `workspace_required`, ask which listed workspace to use and retry with its ID.
- Use `openwiki_list_workspaces` or `openwiki_list_wikis` when workspace membership itself needs to be discovered.
- If the retrieval tools are unavailable, read `openwiki/quickstart.md` and follow its links to the relevant pages.
- Treat source code and tests as authoritative. A brief's unknowns and review items are verification gaps, not automatic requirements.
- Prefer the narrowest quiet validation that proves the changed behavior. Preserve complete failure output.

Use the repository's documented maintenance process to refresh generated pages after source changes. Check its configuration and completed runs before relying on automatic updates. Do not hand-edit generated OpenWiki pages unless explicitly asked; prefer updating source code/docs and letting OpenWiki regenerate.

<!-- OPENWIKI:END -->
