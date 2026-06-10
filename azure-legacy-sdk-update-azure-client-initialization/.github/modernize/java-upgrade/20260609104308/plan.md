# Upgrade Plan: azure-client-initialization (20260609104308)

- **Generated**: 2026-06-09 10:43:08
- **HEAD Branch**: modernize/java-20260609184200
- **HEAD Commit ID**: (see git log)

---

## Available Tools

**JDKs**
- JDK 1.8.0_472: `C:\Users\xiaofeicao\.jdk\jdk-8\bin` (current project JDK, used for baseline in step 2)
- JDK 21.0.6: `C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7\bin` (JAVA_HOME, target — used from step 3 onward)

**Build Tools**
- Maven 3.8.9: `C:\Users\xiaofeicao\.maven\maven-3.8.9\apache-maven-3.8.9\bin` (used throughout; no wrapper present)

> No Maven wrapper present in the project root.

---

## Guidelines

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

- Upgrade JDK from 1.8 to 21 (latest LTS available on this system)
- Evaluate and apply Jakarta EE migration (javax.* → jakarta.*) — no javax.* EE API usage found in source, so this is effectively a no-op at the source level
- Update Maven build/compiler configuration accordingly
- Project must build successfully and all existing tests must pass (no test files exist; compilation success is sufficient)

---

## Options

- Working branch: modernize/java-20260609184200
- Run tests before and after the upgrade: true

---

## Upgrade Goals

1. **JDK**: 1.8 → **21** (latest LTS available)
2. **Jakarta EE**: Migrate any `javax.*` EE usage to `jakarta.*` — no such usage found; documented as verified no-op

---

## Technology Stack

| Technology/Dependency        | Current     | Min Compatible | Why Incompatible                                                 |
|------------------------------|-------------|----------------|------------------------------------------------------------------|
| Java                         | 8           | 21             | User requested latest LTS                                        |
| maven-compiler-plugin        | 3.8.1       | 3.11.0         | 3.11+ recommended for Java 21 support; older versions may not handle release flag correctly |
| exec-maven-plugin            | 1.4.0       | 3.0.0          | Very old (2014); upgrade for better Java 21 compatibility        |
| commons-net                  | 3.3         | 3.9.0          | Old (2014); known CVEs in older versions                         |
| commons-lang ⚠️ EOL          | 2.6         | 2.6            | EOL artifact (last release 2011); no newer version in same GA   |
| commons-lang3                | 3.7         | 3.12.0         | Old (2017); patch upgrades recommended for CVEs                  |
| com.jcraft:jsch ⚠️ EOL      | 0.1.55      | N/A            | Abandoned library; known CVEs; consider replacement              |
| com.microsoft.azure:azure    | 1.36.3      | 1.36.3         | Last release of this legacy SDK; compatible with Java 21 at compile time |

---

## Derived Upgrades

| Derived Upgrade              | Reason                                                                                      |
|------------------------------|---------------------------------------------------------------------------------------------|
| maven-compiler-plugin 3.8.1 → 3.13.0 | Java 21 requires `<release>21</release>`; 3.11+ handles this cleanly              |
| exec-maven-plugin 1.4.0 → 3.4.1 | Old 1.x plugin has known compatibility issues with newer Maven/JDK versions           |
| commons-net 3.3 → 3.11.1    | Security hardening; many bug fixes since 3.3                                               |
| commons-lang3 3.7 → 3.17.0  | CVE fixes and compatibility improvements                                                    |
| No javax.→jakarta. migration | Thorough search of `src/**/*.java` confirmed zero `javax.*` imports — Jakarta EE migration is a verified no-op for this project |

---

## Impact Analysis

### Dependency Changes

| File    | Dependency                              | Current | Action  | Target   | Reason                                              |
|---------|-----------------------------------------|---------|---------|----------|-----------------------------------------------------|
| pom.xml | maven-compiler-plugin `<source>`        | 1.8     | upgrade | 21       | Target JDK 21                                       |
| pom.xml | maven-compiler-plugin `<target>`        | 1.8     | upgrade | 21       | Target JDK 21                                       |
| pom.xml | maven-compiler-plugin version           | 3.8.1   | upgrade | 3.13.0   | Best Java 21 support; use `<release>` tag           |
| pom.xml | exec-maven-plugin version               | 1.4.0   | upgrade | 3.4.1    | Current stable; Java 21 compatibility               |
| pom.xml | commons-net version                     | 3.3     | upgrade | 3.11.1   | Security fixes; compatibility                       |
| pom.xml | commons-lang3 version                   | 3.7     | upgrade | 3.17.0   | Security and bug fixes                              |

> **commons-lang:2.6** is the last release of this EOL artifact. It remains unchanged but is noted as a risk.  
> **com.jcraft:jsch:0.1.55** is abandoned; no upstream patch. Retained as-is; flagged in CVE scan.  
> **com.microsoft.azure:azure:1.36.3** is the last release of the legacy SDK. No version bump needed; it compiles with Java 21.

### Source Code Changes

| File | Location | Current | Required Change | Reason |
|------|----------|---------|----------------|--------|
| (none) | — | — | No source changes needed | No removed Java 8 APIs used; no javax.* EE imports found |

### Configuration Changes

_(No application.properties or application.yml present.)_

### CI/CD Changes

_(No Dockerfile, workflow files, or pipeline configs found in this project.)_

### Risks & Warnings

- **com.microsoft.azure:azure:1.36.3 runtime compatibility**: This SDK was built for Java 7/8. At compile time it is fine with Java 21, but at runtime (if ever executed), Netty/OkHttp and reflection-based serialization might trigger strong-encapsulation errors. **Mitigation**: This project has no runtime tests; compilation success is the target outcome. Runtime hardening (e.g., `--add-opens`) would be a follow-on task.
- **com.jcraft:jsch:0.1.55**: Abandoned; known CVEs. No upstream patch available. **Mitigation**: CVE scan will flag it; if needed, replace with community fork `com.github.mwiede:jsch`.
- **commons-lang:2.6**: EOL artifact. Retained since no drop-in replacement within the same group/artifact. **Mitigation**: Consider replacing with `commons-lang3` long-term; outside scope of this upgrade.
- **javax.* Jakarta EE**: Confirmed zero `javax.*` EE imports in source. Migration is a verified no-op.

---

## Upgrade Steps

- **Step 1: Setup Environment**
  - **Rationale**: Verify all required JDKs and Maven are available before any changes
  - **Changes to Make**: No file changes; verify JDK 8 and JDK 21 and Maven 3.8.9 are accessible
  - **Verification**: `C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7\bin\java.exe -version`, Maven 3.8.9 available

- **Step 2: Setup Baseline**
  - **Rationale**: Record baseline build status with JDK 8 before upgrade; no tests exist so compilation is the baseline
  - **Changes to Make**: No file changes
  - **Verification**: `mvn clean compile test-compile -q` with JDK 8 at `C:\Users\xiaofeicao\.jdk\jdk-8\bin`; Expected: SUCCESS

- **Step 3: Upgrade JDK and Build Configuration to Java 21**
  - **Rationale**: Update pom.xml to target Java 21; upgrade compiler and exec plugins
  - **Changes to Make**: All Dependency Changes from Impact Analysis — maven-compiler-plugin source/target → 21 and version → 3.13.0; exec-maven-plugin → 3.4.1
  - **Verification**: `mvn clean test-compile -q` with JDK 21; Expected: COMPILATION SUCCESS

- **Step 4: Update Dependency Versions**
  - **Rationale**: Upgrade old vulnerable/stale dependency versions (commons-net, commons-lang3)
  - **Changes to Make**: commons-net 3.3 → 3.11.1; commons-lang3 3.7 → 3.17.0
  - **Verification**: `mvn clean test-compile -q` with JDK 21; Expected: COMPILATION SUCCESS

- **Step 5: CVE Validation & Fix**
  - **Rationale**: Scan direct dependencies for known CVEs; fix any found by patching versions
  - **Changes to Make**: Per CVE scan results
  - **Verification**: Re-scan confirms no remaining fixable CVEs; `mvn clean test-compile -q` succeeds

- **Step 6: Final Validation**
  - **Rationale**: Full clean build confirming all upgrade goals met
  - **Changes to Make**: Resolve any remaining TODOs/workarounds
  - **Verification**: `mvn clean test-compile -q` with JDK 21; Expected: BUILD SUCCESS (no test files exist, so compilation is the full validation)
