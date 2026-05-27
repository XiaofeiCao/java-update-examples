# Modernization Plan: Azure SDK Upgrade

## Project Overview

| Property | Value |
|----------|-------|
| **Project** | azure-client-initialization |
| **Language** | Java 1.8 |
| **Build Tool** | Maven |
| **Assessment Report** | report-20260527115123 |

## Scope

This plan addresses the migration of legacy Azure SDKs for Java (`com.microsoft.azure.*`) to the latest Azure SDK (`com.azure.*`). The legacy Azure SDK has reached end of support and must be upgraded to continue receiving security patches and new features.

## Current State

- **Legacy Dependency**: `com.microsoft.azure:azure:1.36.3`
- **Affected Files**:
  - `src/main/java/com/microsoft/azure/management/clientinitialization/AzureInitialization.java`
  - `src/main/java/com/microsoft/azure/management/resources/core/ResourceGroupTaggingInterceptor.java`
  - `pom.xml`

## Tasks

### Task 001: Upgrade from Legacy Azure SDKs for Java to the latest

| Property | Value |
|----------|-------|
| **ID** | 001-upgrade-azure-legacy-sdk |
| **Type** | transform |
| **Knowledge Base** | azure-legacy-java-sdk-upgrade |

**Description**: Migrate from legacy Azure SDK (`com.microsoft.azure:azure:1.36.3`) to the latest Azure SDK for Java (`com.azure.resourcemanager`).

**What will change**:
1. Replace `com.microsoft.azure:azure` dependency with `com.azure.resourcemanager:azure-resourcemanager` in `pom.xml`
2. Update authentication patterns from legacy `ApplicationTokenCredentials` to modern `DefaultAzureCredential` or `ClientSecretCredential`
3. Replace `Azure.configure()...authenticate()` initialization with `AzureResourceManager.configure()...authenticate()`
4. Update all Azure resource management API calls to use the new SDK equivalents
5. Update interceptor patterns to align with the new SDK's HTTP pipeline

**Success Criteria**:
- Project builds successfully with `mvn compile`
- All unit tests pass

## Execution Order

1. **001-upgrade-azure-legacy-sdk** — No dependencies, can execute immediately

## Notes

- The legacy `com.microsoft.azure:azure` SDK reached end of support. The new `com.azure.resourcemanager` SDK provides improved performance, better error handling, and continued support.
- Authentication patterns change significantly between the old and new SDKs.
