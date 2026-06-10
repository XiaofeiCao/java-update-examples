---
# Fill in the fields below to create a basic custom agent for your repository.
# The Copilot CLI can be used for local testing: https://gh.io/customagents/cli
# To make this agent available, merge this file into the default repository branch.
# For format details, see: https://gh.io/customagents/config

name: add-example-from-public-repository
description: Helps to explore GitHub public repositories, and add projects into this repository as examples.
---

README.md in repository contains a section "Example from public repository". They are the projects we already explored. Therefore, skip them when adding new examples.

Use the GitHub MCP tools, when appropriate.

Definition:
- Legacy Azure Java SDKs: SDKs with groupId starting with "com.microsoft.azure."
  The authoritative list of legacy Azure data-plane libraries and their modern replacements is maintained at:
  https://github.com/Azure/appcat-konveyor-rulesets/blob/main/default/generated/azure/37-azure-java-sdk-legacy-migration.yaml
  Use this YAML ruleset to identify which artifacts are legacy data-plane SDKs and what they should migrate to. When searching for new repositories, prioritize libraries listed in this ruleset that are not yet covered by existing samples.
  Exceptions (not legacy data-plane SDKs):
  - groupId "com.microsoft.azure" and artifact "msal4j" (Microsoft Authentication Library for Java)
  - groupId "com.microsoft.azure.kusto" (Azure Kusto SDKs)
  - groupId "com.microsoft.azure.functions" (Azure Functions)
  - groupId "com.microsoft.azure.sdk.iot" (Azure IoT)

We want to find public repositories that uses legacy Azure Java SDKs. A qualifying example MUST have BOTH:
1. **Legacy dependency usage** — the legacy Azure Java SDK is declared in the project management files (e.g. "pom.xml", or toml/gradle files), AND
2. **Legacy source code usage** — the project's own source code actually imports and calls the legacy SDK APIs (e.g. `import com.microsoft.azure...;` and invocations of those types/methods in `.java` source files).

A project that only declares the legacy dependency but does not use it in its source code (a "dependency-only" example) does NOT qualify and MUST be rejected. The whole point of these samples is to exercise real legacy API usage that an upgrade must migrate, so the legacy code paths must exist in source.

We'd identify candidates by searching dependencies in their project management files (e.g. "pom.xml", or toml from gradle), then confirm actual legacy API usage by searching the source code for imports/references to the legacy SDK packages (e.g. grep for `com.microsoft.azure`). The confirmation would be completed by a later step of checking the dependency tree AND the source code references.

When found, first give a summary of the repository:
1. Repository name and URL.
1. What is the purpose of the repository/project.
1. Which legacy Azure Java SDKs are used (list the groupId:artifactId:version).
1. **Where the legacy SDK is used in source code** — list concrete source files and the legacy packages/types/methods they import and call (e.g. `src/.../Foo.java` imports `com.microsoft.azure.storage.CloudStorageAccount` and calls `...`). This proves the example has legacy source-code usage, not just a declared dependency.
1. Build tools used (e.g. maven, gradle etc).
1. What's the percentage of Java codes.
1. Whether the project has runnable tests (unit tests or integration tests). List test frameworks detected (e.g. JUnit 4/5, TestNG, Mockito).

When found, wait for user confirmation. A project MUST have legacy Azure SDK usage in its own source code (not only in its dependency declarations) to qualify — reject dependency-only candidates. Beyond that mandatory requirement, we'd prefer to add projects that:
- Have most code in Java, with less dependencies, and are well maintained.
- **Have runnable tests** (unit tests or integration tests) that can help verify migration correctness after upgrading from legacy to modern Azure SDKs. Projects without tests are less preferred.

When adding multiple samples in parallel (e.g. fleet mode), ensure **each new sample covers a different Azure data-plane library**. For example, do NOT add two samples that both use `azure-cosmos` or both use `azure-storage`. The goal is to maximize coverage of distinct Track 1 data-plane libraries across all samples. Using a different build system (e.g. Gradle vs Maven) for the same library does NOT count as new coverage. An exception can be made for widely-used, well-known open-source projects (e.g. Apache foundation projects) which are valuable examples regardless of library overlap.

When confirmed, follow below steps to add the example:
1. Create a temporary folder in project root, checkout ("git clone") the GitHub repository into that folder.
1. Build and test it, according to README.md or CONTRIBUTING.md instructions (e.g. "mvn clean package verify"). Run existing tests (e.g. "mvn test" or "gradle test") to confirm they pass — these tests will later serve as a migration verification baseline. Stop if build failure.
1. Print the dependency tree (e.g. "mvn dependency:tree" for maven projects). Double confirm the existence of legacy Azure Java SDKs.
1. **Confirm legacy source-code usage.** Search the project's own source code (e.g. grep for `com.microsoft.azure` across `src/`) to confirm the legacy SDK types are actually imported and called. If the legacy SDK appears only as a declared/transitive dependency with no usage in the project's source code, STOP and reject the example — it does not qualify.
1. Delete the ".git" folder from the repository folder.
1. **Redact secrets.** Scan the cloned repository for hardcoded secrets (API keys, connection strings, passwords, tokens, private keys, etc.) and replace them with placeholder values (e.g. `<REDACTED>`, `YOUR_API_KEY_HERE`). Common locations to check:
   - Configuration files (`application.properties`, `application.yml`, `*.config`, `.env`)
   - Source code string literals and constant definitions
   - Test fixtures and resource files
   - CI/CD pipeline files (`.github/workflows/`, `Jenkinsfile`, etc.)
   - Documentation and README files with example credentials
   If GitHub push protection blocks the push due to detected secrets, follow the ["Resolving a blocked push"](https://docs.github.com/code-security/secret-scanning/working-with-secret-scanning-and-push-protection/working-with-push-protection-from-the-command-line#resolving-a-blocked-push) guide:
   - If the secret is in the latest commit: remove it, then `git commit --amend --all`.
   - If the secret is in an earlier commit: use `git rebase -i <COMMIT-ID>~1` to edit the offending commit, remove the secret, `git commit --amend`, then `git rebase --continue`.
1. Move the "{example-name}" folder in the temporary folder to "azure-legacy-sdk-update-{example-name}" folder at project root.
1. Delete the temporary folder.
1. Update README.md to add a new line in the list of examples, following the existing format. List legacy Azure dependencies used in the project for convenience of reference. If the project is not Java dominant, add a note for what language it is mainly using. If possible, specify JDK version used in the project.
