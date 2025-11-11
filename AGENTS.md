# AGENTS.md
 
## Setup commands
- Compile application: `gradle build`
- Run tests: `gradle test`
 
## Dev environment 
- Java 21
- Gradle
- Nothing can be commited or pushed to the main branch
- All changes must occur on feature branches
- Before commiting or pushing code, the code must compile and run successfully
- Agents may create pull requests, at least one authorized human must review and approve all pull requests
- Code commits must be very narrowly scoped, do not change anything outside of the strict narrow scope of the ask
- No commit may create a SonarQube bug or a SonarQube security violation

## Coding conventions
- The use of Lombok is preferred over writing custom code
- Existing classes and methods from Apache Commons or Spring is preferred over writing custom code
- Variable names should be meaningful

## Pull requests
- Pull requests must state the reason for the change, that the change was created by an AI, and the models used must be listed in the PR title.

