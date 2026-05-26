# Modernization Plan: azure-client-initialization

**Project**: azure-client-initialization

---

## Technical Framework

- **Language**: Java 1.8 (upgrading to Java 17 LTS)
- **Framework**: N/A (plain Java application)
- **Build Tool**: Maven
- **Database**: N/A
- **Key Dependencies**: `com.microsoft.azure:azure:1.36.3` (legacy Azure SDK), `commons-net`, `commons-lang3`, `jsch`

---

## Overview

This migration upgrades the `azure-client-initialization` project from end-of-support Java 1.8 and legacy Azure SDK (`com.microsoft.azure.*`) to modern, supported equivalents. The application currently initializes Azure management clients using the deprecated `com.microsoft.azure` SDK with `ApplicationTokenCredentials`, a custom `RestClient.Builder`, and file-based credential loading. The new architecture will:

- Upgrade the Java runtime from 1.8 to 17 LTS for long-term security and compatibility support
- Replace all `com.microsoft.azure.*` legacy SDK imports and APIs with the modern `com.azure.*` Azure SDK for Java (Track 2), including credential management via `DefaultAzureCredential` and updated resource management clients
- Remediate known CVEs in project dependencies to ensure security compliance before deployment

The migration follows a sequential approach: runtime upgrade first, then SDK migration, then security hardening.

---

## Migration Impact Summary

| Application              | Original Service                        | New Azure Service                     | Authentication       | Comments                                      |
|--------------------------|-----------------------------------------|---------------------------------------|----------------------|-----------------------------------------------|
| azure-client-initialization | Legacy Azure SDK (`com.microsoft.azure:azure` 1.36.3) | Modern Azure SDK (`com.azure:azure-resourcemanager`) | Managed Identity / DefaultAzureCredential | Replace ApplicationTokenCredentials, RestClient.Builder, and file-based auth |

---

## Open Questions & Questionnaire

- [x] Q: Which Java version should be targeted? → A: Java 17 LTS
- [x] Q: Should the plan include deployment? → A: No — migration only, no cloud deployment
- [x] Q: Should the plan include security/CVE remediation? → A: Yes — include security/CVE remediation
