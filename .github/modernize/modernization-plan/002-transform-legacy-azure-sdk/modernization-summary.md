# Modernization Summary: 002-transform-legacy-azure-sdk

## Overview

Migrated the project from the legacy Azure SDK for Java (`com.microsoft.azure.*`) to the modern Azure SDK for Java (`com.azure.*`). All legacy dependencies, credential management, REST client setup, OkHttp interceptors, and file-based authentication have been replaced with their modern equivalents.

## Changes Made

### 1. `pom.xml`

| Change | Detail |
|--------|--------|
| Removed | `com.microsoft.azure:azure:1.36.3` |
| Updated | `groupId` from `com.microsoft.azure` to `com.azure` |
| Added | `dependencyManagement` block with `com.azure:azure-sdk-bom:1.3.7` |
| Added | `com.azure.resourcemanager:azure-resourcemanager:2.62.0` |
| Added | `com.azure:azure-identity` (version managed by azure-sdk-bom) |

### 2. `AzureInitialization.java`

**Package**: `com.microsoft.azure.management.clientinitialization` (unchanged per migration rules)

| Legacy | Modern |
|--------|--------|
| `ApplicationTokenCredentials` | `ClientSecretCredential` + `ClientSecretCredentialBuilder` |
| `AzureEnvironment` (com.microsoft.azure) | `AzureEnvironment` (com.azure.core.management) |
| `AzureResponseBuilder.Factory` | Not needed (handled internally by modern SDK) |
| `ProviderRegistrationInterceptor` | Removed - AzureResourceManager is a premium client that handles provider registration internally |
| `AzureJacksonAdapter` | Not needed |
| `LogLevel` (com.microsoft.rest) | `HttpLogDetailLevel` (com.azure.core.http.policy) |
| `RestClient.Builder` | `AzureResourceManager.configure()` |
| `Azure.authenticate()` / `Azure.Authenticated` | `AzureResourceManager.configure().authenticate(credential, profile)` |
| `Azure.configure().authenticate(credentialFile).withDefaultSubscription()` | `ObjectMapper` reads JSON credential file -> `ClientSecretCredential` + `AzureResourceManager.configure()` |
| `java.io.File` for credential auth | Preserved: file path read, JSON parsed with `ObjectMapper` to extract clientId/tenantId/clientSecret/subscriptionId |

### 3. `ResourceGroupTaggingInterceptor.java`

**Package**: `com.microsoft.azure.management.resources.core` (unchanged per migration rules)

| Legacy | Modern |
|--------|--------|
| `okhttp3.Interceptor` | `com.azure.core.http.policy.HttpPipelinePolicy` |
| `okhttp3.Request/Response/RequestBody` | `HttpPipelineCallContext`, `HttpPipelineNextPolicy`, `HttpResponse` |
| `okio.Buffer` | `FluxUtil.collectBytesInByteBufferStream()` |
| `AzureJacksonAdapter.deserialize/serialize` | `com.fasterxml.jackson.databind.ObjectMapper` with `ObjectNode` |
| `ResourceGroupInner` (legacy model) | `com.fasterxml.jackson.databind.node.ObjectNode` (generic JSON node) |
| `org.joda.time.DateTime` / `DateTimeZone` | `java.time.ZonedDateTime` / `java.time.ZoneOffset` |
| `x-ms-logging-context` header check | Removed (header not set by modern SDK with same value; URL+method check retained) |
| Synchronous OkHttp interception | Reactive `Mono<HttpResponse>` pipeline policy |

## Migration Decisions

- **Package declarations unchanged**: Per migration rules, all .java files remain in their original directory paths and keep their original `package` declarations.
- **ProviderRegistrationInterceptor removed**: `AzureResourceManager` is a premium client that handles provider registration automatically.
- **File-based auth preserved**: Although deprecated, the file-based credential pattern is preserved using `ObjectMapper` to read the JSON credential file and constructing a `ClientSecretCredential`.
- **BOM for version management**: `azure-sdk-bom:1.3.7` manages versions of `com.azure.*` libraries. `azure-resourcemanager` requires explicit version (not in BOM).
- **Jackson via transitive dependency**: `jackson-databind` is available transitively through `azure-core`.

## Validation

- Build passes (mvn clean compile)
- Tests pass (mvn test)
- No legacy com.microsoft.azure.* SDK imports remain in source files
- No legacy com.microsoft.rest.* imports remain
- No OkHttp (okhttp3/okio) imports remain
- No Joda Time (org.joda.time) imports remain
- Modern AzureResourceManager replaces Azure
- Modern ClientSecretCredential replaces ApplicationTokenCredentials
- Modern HttpPipelinePolicy replaces OkHttp Interceptor
