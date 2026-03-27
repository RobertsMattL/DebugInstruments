# CodeFlow Android Project

## Build Validation Policy

**IMPORTANT: Under no circumstances should you validate your changes using Gradle building.**

Do not run `./gradlew build`, `./gradlew assembleDebug`, or any other Gradle build commands to verify your code changes. The CI/CD pipeline will handle build validation - if there are build failures, they will be reported through GitHub Actions notifications.

If a build failure is reported:
1. Query the GitHub API to retrieve the workflow run details and logs
2. Analyze the failure output to identify the root cause
3. Fix the identified issues in the code

Trust that your code changes are syntactically correct without running local builds.
