# Migration Plan

**Session ID**: ff1f74ea-effe-49fd-a773-d0562a4f1b81  
**Created**: 2026-06-09 18:51:52  
**KB ID**: azure-legacy-java-sdk-upgrade  
**Language**: Java (confirmed — pom.xml + .java files present)  
**Branch**: modernize/java-20260609184200  
**Uncommitted Changes Policy**: Always Stash  

## Guidelines

- **TARGET_AZURE_SDK_BOM_VERSION = 1.3.7** (resolved from https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/boms/azure-sdk-bom/pom.xml)
- Migration Guide: https://aka.ms/java-track2-migration-guide (com.microsoft.azure.management.** → com.azure.resourcemanager.**)
- OKHttp Interceptors → HttpPipelinePolicy (two-step: convert class, register via .withPolicy())
- ProviderRegistrationInterceptor: `Azure` IS in the list, so no ProviderRegistrationPolicy needed
- File-based auth → DefaultAzureCredential (with TODO comment)
- Env-var auth → DefaultAzureCredential (preferred Managed Identity)

## Build Environment

### JDK Settings
- JDK version: 21 (as specified in pom.xml `<release>21</release>`)
- JAVA_HOME: `C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7` (JDK 21.0.6, set as JAVA_HOME in system environment)
- Need to install new JDK: No (JDK 21.0.6 already available at JAVA_HOME)

### Build Tool Settings
- Build tool: Maven
- Wrapper used: No
- MAVEN_HOME: `C:\Users\xiaofeicao\.maven\maven-3.8.9\apache-maven-3.8.9` (Maven 3.8.9)

## Files to Change (in dependency order)

1. **`pom.xml`** — no dependencies on other files; must be changed first
2. **`src/main/java/com/microsoft/azure/management/resources/core/ResourceGroupTaggingInterceptor.java`** → create `ResourceGroupTaggingPolicy.java`, delete old file; no dependencies on AzureInitialization.java
3. **`src/main/java/com/microsoft/azure/management/clientinitialization/AzureInitialization.java`** — depends on ResourceGroupTaggingPolicy

## Migration Details

### pom.xml
- Remove `com.microsoft.azure:azure:1.36.3`
- Add `<dependencyManagement>` with `com.azure:azure-sdk-bom:1.3.7`
- Add `com.azure.resourcemanager:azure-resourcemanager` (no version — BOM-managed)
- Add `com.azure:azure-identity` (no version — BOM-managed)
- Retain `commons-net`, `commons-lang`, `commons-lang3`, `jsch` (unchanged)

### ResourceGroupTaggingInterceptor.java → ResourceGroupTaggingPolicy.java
- Rename class `ResourceGroupTaggingInterceptor` → `ResourceGroupTaggingPolicy`
- Rename file to `ResourceGroupTaggingPolicy.java` (same directory, same package declaration)
- Replace `implements Interceptor` → `implements HttpPipelinePolicy`
- Replace `intercept(Chain chain)` → `process(HttpPipelineCallContext, HttpPipelineNextPolicy)` returning `Mono<HttpResponse>`
- Replace `AzureJacksonAdapter.deserialize/serialize` → `ObjectMapper` (Jackson, transitive dep of azure-core)
- Replace `ResourceGroupInner` → raw JSON manipulation with `ObjectNode`
- Replace `joda.time.DateTime/DateTimeZone` → `java.time.OffsetDateTime`
- Replace `okhttp3.*` / `okio.*` imports → `com.azure.core.http.*` imports
- Delete old `ResourceGroupTaggingInterceptor.java`

### AzureInitialization.java
- Replace `ApplicationTokenCredentials` → `DefaultAzureCredentialBuilder().build()` (TokenCredential)
- Replace `AzureEnvironment` → `com.azure.core.management.AzureEnvironment`
- Add `AzureProfile` from `com.azure.core.management.profile.AzureProfile`
- Replace `Azure.configure()...authenticate()` → `AzureResourceManager.configure().withPolicy(new ResourceGroupTaggingPolicy()).authenticate(credential, profile).withSubscription(subscriptionId)`
- Remove `ProviderRegistrationInterceptor` (Azure is in the list — not needed in new SDK)
- Remove `RestClient.Builder` and related legacy imports
- Remove file-based auth section (`Azure.configure().authenticate(credentialFile)...`) — add TODO comment
- Update validation guard to check only `AZURE_SUBSCRIPTION_ID`
- Remove `throws IOException` from main method signature

## Validation & Fix Steps

### Stage 1: Build and Fix Loop (Up to 10 rounds)
### Stage 2: CVE Validation and Fix
### Stage 3: Consistency Validation and Fix
### Stage 4: Test Validation and Fix
### Stage 5: Completeness Validation and Fix
### Stage 6: Build Validation (Final Check)
