# Migration Plan: Azure Legacy Java SDK Upgrade

## Migration Session Information
- **Migration Session ID**: e0386ea8-e894-4bfd-994d-235f567e7fbb
- **Plan Creation Time**: 2026-05-27 15:03:48
- **Uncommitted Changes Policy**: Always Stash
- **Target Branch Name**: `appmod/java-azure-legacy-java-sdk-upgrade-20260527150348`
- **Programming Language**: Java
- **Knowledge Base ID**: azure-legacy-java-sdk-upgrade

## Source Technology Verification
✅ **Source technology confirmed**: The workspace uses the legacy Azure SDK (`com.microsoft.azure:azure:1.36.3`) with legacy imports such as:
- `com.microsoft.azure.AzureEnvironment`
- `com.microsoft.azure.credentials.ApplicationTokenCredentials`
- `com.microsoft.azure.management.Azure`
- `com.microsoft.rest.RestClient`
- `com.microsoft.azure.serializer.AzureJacksonAdapter`
- `com.microsoft.azure.AzureResponseBuilder`

## Migration Overview
This migration upgrades the project from the legacy Azure Management SDK (`com.microsoft.azure:azure:1.36.3`) to the new Azure Resource Manager SDK (`com.azure.resourcemanager:azure-resourcemanager`). The new SDK provides:
- Azure Identity-based authentication (`com.azure.identity`)
- Fluent resource management API (`com.azure.resourcemanager`)
- Modern HTTP pipeline with `com.azure.core`

## Files to be Changed (Dependency Order)

### 1. `pom.xml` (No dependencies - modify first)
- **Reason**: Build configuration and dependencies must be updated before source files can compile against the new SDK.
- **Changes**:
  - Replace `com.microsoft.azure:azure:1.36.3` with `com.azure.resourcemanager:azure-resourcemanager` (latest stable)
  - Add `com.azure:azure-identity` dependency
  - Update Java source/target version if needed
  - Remove unused legacy dependencies related to Azure SDK

### 2. `src/main/java/com/microsoft/azure/management/resources/core/ResourceGroupTaggingInterceptor.java` (Depends on pom.xml)
- **Reason**: This file uses legacy SDK classes (`AzureJacksonAdapter`, `ResourceGroupInner`, `okhttp3.Interceptor`). It must be migrated before `AzureInitialization.java` which references it.
- **Changes**:
  - Replace legacy `com.microsoft.azure.serializer.AzureJacksonAdapter` with new serialization approach
  - Replace `com.microsoft.azure.management.resources.implementation.ResourceGroupInner` with new SDK equivalent
  - Update OkHttp interceptor pattern to new HTTP pipeline policy pattern using `com.azure.core.http.policy.HttpPipelinePolicy`
  - Update imports from `okhttp3.*` to Azure Core HTTP classes
  - Remove `org.joda.time` usage, replace with `java.time` equivalents

### 3. `src/main/java/com/microsoft/azure/management/clientinitialization/AzureInitialization.java` (Depends on pom.xml and ResourceGroupTaggingInterceptor.java)
- **Reason**: This file depends on `ResourceGroupTaggingInterceptor` and uses legacy authentication and client initialization patterns.
- **Changes**:
  - Replace `com.microsoft.azure.credentials.ApplicationTokenCredentials` with `com.azure.identity.ClientSecretCredentialBuilder`
  - Replace `com.microsoft.azure.management.Azure` with `com.azure.resourcemanager.AzureResourceManager`
  - Replace `com.microsoft.rest.RestClient` with new HTTP pipeline configuration
  - Replace `AzureEnvironment` with `com.azure.core.management.AzureEnvironment`
  - Update authentication flow to use `TokenCredential` from Azure Identity
  - Replace file-based authentication with `Azure Identity` credential providers
  - Remove `com.microsoft.azure.AzureResponseBuilder`, `AzureJacksonAdapter`, `ProviderRegistrationInterceptor` references
  - Update `LogLevel` to new HTTP logging configuration

## Search Patterns Used
- `**/*.java` with query `com.microsoft.azure` → 2 files found
- `**/pom.xml` with query `com.microsoft.azure` → 1 file found

## Knowledge Base Guidelines
- **Azure Legacy Java SDK Upgrade** (KB ID: azure-legacy-java-sdk-upgrade)
  - Migrate from `com.microsoft.azure` to `com.azure.resourcemanager`
  - Replace `ApplicationTokenCredentials` with Azure Identity credentials
  - Replace `RestClient` with Azure Core HTTP pipeline
  - Replace legacy fluent management API with new Resource Manager API

## Build Environment Settings

### JDK Settings
- **JDK Version (Project)**: 1.8 (Java 8)
  - **Reason**: The `pom.xml` specifies `<source>1.8</source>` and `<target>1.8</target>` in the `maven-compiler-plugin` configuration.
- **Need to install a new JDK**: false
  - **Reason**: A suitable JDK 8 (version 1.8.0_472) is already installed at `C:\Users\xiaofeicao\.jdk\jdk-8\bin`.
- **JAVA_HOME**: `C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7`
  - **Reason**: This is the JDK configured as JAVA_HOME in the system environment. Java 21 is an LTS version and is higher than the project's required Java 8, making it suitable for building the project. The JAVA_HOME environment variable takes priority over other installed JDKs.
- **Path to install a new JDK**: N/A
- **JDK version to install**: N/A

### Build Tool Settings
- **Build Tool Type**: maven
- **Is Wrapper Used**: false
- **MAVEN_HOME**: `C:\Users\xiaofeicao\.maven\apache-maven-3.8.1`
  - **Reason**: This Maven installation is configured in MAVEN_HOME and PATH system environment variables, making it the preferred Maven instance.
- **Path to install maven**: N/A
