# Azure Legacy Java SDK Migration Plan

## Session Information

| Field | Value |
|---|---|
| Migration Session ID | 809c44c3-7659-4a94-a3b4-5c385138a447 |
| Generated At | 2026-06-09 18:05:50 |
| Uncommitted Changes Policy | Always Stash |
| Target Branch | `appmod/java-azure-legacy-java-sdk-upgrade-20260609180550` |
| Programming Language | Java |
| KB ID | azure-legacy-java-sdk-upgrade |

---

## Source Technology Verification

✅ **Legacy Azure SDK confirmed present.**

| Evidence | Location |
|---|---|
| `com.microsoft.azure:azure:1.36.3` dependency | `pom.xml` |
| `import com.microsoft.azure.management.Azure` | `AzureInitialization.java` |
| `import com.microsoft.azure.credentials.ApplicationTokenCredentials` | `AzureInitialization.java` |
| `import com.microsoft.rest.RestClient` | `AzureInitialization.java` |
| `import com.microsoft.azure.management.resources.fluentcore.utils.ProviderRegistrationInterceptor` | `AzureInitialization.java` |
| OKHttp `Interceptor` implementation | `ResourceGroupTaggingInterceptor.java` |
| `import com.microsoft.azure.serializer.AzureJacksonAdapter` | `ResourceGroupTaggingInterceptor.java` |

---

## Guidelines

- **TARGET_AZURE_SDK_BOM_VERSION = 1.3.7** *(resolved from https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/boms/azure-sdk-bom/pom.xml at plan-generation time)*
- Migration guide used: [Migrate to `com.azure.resourcemanager.**` from `com.microsoft.azure.management.**`](https://aka.ms/java-track2-migration-guide)
- `Azure` is in the built-in ProviderRegistration list — do **NOT** add `ProviderRegistrationPolicy` in the migrated code.
- File-based auth (`Azure.configure().authenticate(credentialFile)` / `AZURE_AUTH_LOCATION`) **must** be replaced with `DefaultAzureCredential` and a `TODO` comment explaining the change; do **not** reproduce the file-reading pattern.
- OKHttp `Interceptor` classes must be converted to `HttpPipelinePolicy` classes (two-step migration — convert class AND wire via `.withPolicy(...)`).
- Do **not** change `package` declarations or move/rename source files; only update `import` statements and type usages inside the file body.
- Do not upgrade JDK version (project already targets Java 1.8, which is >= 8).

---

## Build Environment Settings

| Setting | Value |
|---|---|
| Project JDK Version | Java 1.8 (from `maven-compiler-plugin` `<source>1.8</source>`) |
| Suitable JDK (LTS ≥ 1.8) | Java 21.0.6 — `C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7` (JAVA_HOME) |
| JAVA_HOME | `C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7` |
| New JDK Installation Required | No |
| New JDK Version to Install | N/A |
| Build Tool | Maven (`pom.xml` present, no Gradle) |
| Maven Wrapper | No |
| MAVEN_HOME | `C:\Users\xiaofeicao\.maven\apache-maven-3.8.1` (from MAVEN_HOME env var) |
| Maven Version Used | 3.8.1 |
| Maven Install Required | No |

---

## Files to be Changed (in dependency order)

Files with no inter-project dependencies are listed first; files that depend on those come after.

| Order | File | Reason |
|---|---|---|
| 1 | `pom.xml` | Remove legacy `com.microsoft.azure:azure:1.36.3`; add azure-sdk-bom BOM and modern `com.azure.resourcemanager:azure-resourcemanager` + `com.azure:azure-identity` dependencies |
| 2 | `src/main/java/com/microsoft/azure/management/resources/core/ResourceGroupTaggingInterceptor.java` | Implements OKHttp `Interceptor`; must be converted to an `HttpPipelinePolicy` implementation (class renamed to `ResourceGroupTaggingPolicy`); no dependency on `AzureInitialization` |
| 3 | `src/main/java/com/microsoft/azure/management/clientinitialization/AzureInitialization.java` | Depends on `ResourceGroupTaggingInterceptor` (now `ResourceGroupTaggingPolicy`); uses legacy `ApplicationTokenCredentials`, `RestClient.Builder`, `Azure`, `ProviderRegistrationInterceptor`, and file-based auth — all must be modernized |

---

## Migration Tasks

### Task 1 — Update `pom.xml`

**Search patterns**: `com.microsoft.azure:azure`, `<artifactId>azure</artifactId>`

**Changes required**:

1. Add `<dependencyManagement>` block importing the azure-sdk-bom (TARGET_AZURE_SDK_BOM_VERSION = 1.3.7):

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.azure</groupId>
      <artifactId>azure-sdk-bom</artifactId>
      <version>1.3.7</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

2. Remove the legacy `com.microsoft.azure:azure` dependency entirely:

```xml
<!-- REMOVE THIS -->
<dependency>
  <groupId>com.microsoft.azure</groupId>
  <artifactId>azure</artifactId>
  <version>1.36.3</version>
</dependency>
```

3. Add modern replacements (no explicit version — managed by BOM):

```xml
<dependency>
  <groupId>com.azure.resourcemanager</groupId>
  <artifactId>azure-resourcemanager</artifactId>
</dependency>
<dependency>
  <groupId>com.azure</groupId>
  <artifactId>azure-identity</artifactId>
</dependency>
```

---

### Task 2 — Migrate `ResourceGroupTaggingInterceptor.java` → `ResourceGroupTaggingPolicy`

**Search patterns**: `implements Interceptor`, `okhttp3.Interceptor`, `import com.microsoft.azure.serializer`, `import com.microsoft.azure.management.resources.implementation`

**Rules**:
- Keep the file at its current path: `src/main/java/com/microsoft/azure/management/resources/core/ResourceGroupTaggingInterceptor.java`
- Keep the `package com.microsoft.azure.management.resources.core;` declaration unchanged
- Rename the class from `ResourceGroupTaggingInterceptor` to `ResourceGroupTaggingPolicy`
- Implement `HttpPipelinePolicy` instead of OKHttp `Interceptor`
- Rewrite `intercept(Chain chain)` as `process(HttpPipelineCallContext context, HttpPipelineNextPolicy next)`

**New imports** (replace all legacy imports):

```java
import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.util.serializer.JacksonAdapter;
import com.azure.resourcemanager.resources.fluent.models.ResourceGroupInner;
import reactor.core.publisher.Mono;
```

**Migrated class skeleton**:

```java
public class ResourceGroupTaggingPolicy implements HttpPipelinePolicy {

    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
        // Replicate the original interceptor logic:
        // If PUT to /resourcegroups/ with the matching logging context header,
        // deserialize the body, add tags, and re-serialize before proceeding.
        // Otherwise pass through unchanged.
        return next.process();
    }
}
```

> Note: The full body-read/modify/re-write logic from `intercept(Chain)` must be faithfully reproduced in `process(...)` using the Azure Core HTTP pipeline API. Preserve all tag keys (`product`, `cause`, `date`, `job`) and their values exactly.

---

### Task 3 — Migrate `AzureInitialization.java`

**Search patterns**: `import com.microsoft.azure`, `Azure.authenticate`, `Azure.configure`, `RestClient.Builder`, `ApplicationTokenCredentials`, `ProviderRegistrationInterceptor`, `AZURE_AUTH_LOCATION`

**Changes required**:

#### 3a — Replace env-var-based credential and RestClient initialization

Legacy:
```java
ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
    clientId, tenantId, clientSecret, AzureEnvironment.AZURE);
credentials.withDefaultSubscriptionId(subscriptionId);

String baseUrl = credentials.environment().url(AzureEnvironment.Endpoint.RESOURCE_MANAGER);
RestClient.Builder builder = new RestClient.Builder()
    .withBaseUrl(baseUrl)
    .withSerializerAdapter(new AzureJacksonAdapter())
    .withResponseBuilderFactory(new AzureResponseBuilder.Factory())
    .withInterceptor(new ProviderRegistrationInterceptor(credentials))  // Azure is in the built-in list → REMOVE
    .withNetworkInterceptor(new ResourceGroupTaggingInterceptor())
    .withCredentials(credentials)
    .withLogLevel(LogLevel.BODY_AND_HEADERS)
    .withReadTimeout(3, TimeUnit.MINUTES);

Azure.Authenticated azureAuthed = Azure.authenticate(builder.build(), subscriptionId, credentials.domain());
Azure azure = azureAuthed.withSubscription(subscriptionId);
```

Migrated (env vars are NOT file-based auth — no TODO needed here):
```java
TokenCredential credential = new DefaultAzureCredentialBuilder().build();
AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

AzureResourceManager azure = AzureResourceManager.configure()
    .withPolicy(new ResourceGroupTaggingPolicy())
    .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS))
    .authenticate(credential, profile)
    .withDefaultSubscription();
```

> `ProviderRegistrationInterceptor` is removed because `AzureResourceManager` (formerly `Azure`) is in the built-in ProviderRegistration list — adding `ProviderRegistrationPolicy` manually is neither required nor correct.

#### 3b — Replace file-based authentication block

Legacy:
```java
final File credentialFile = new File(System.getenv("AZURE_AUTH_LOCATION"));
azure = Azure.configure()
    .authenticate(credentialFile)
    .withDefaultSubscription();
```

Migrated (replace with `DefaultAzureCredential` + mandatory TODO comment):
```java
// TODO: The original code authenticated using a credential file (AZURE_AUTH_LOCATION),
// which is discouraged because it relies on long-lived secrets on disk and conflicts
// with Azure's security-by-default guidance. It has been replaced with
// DefaultAzureCredential. This change alters the authentication mechanism, so the
// resulting code path requires extra testing (local dev, CI, and target runtime
// identities) before it is considered production-ready.
TokenCredential credential = new DefaultAzureCredentialBuilder().build();
AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
azure = AzureResourceManager.configure()
    .authenticate(credential, profile)
    .withDefaultSubscription();
```

#### 3c — Update error message

The existing validation guard references `AZURE_AUTH_LOCATION`. Rewrite the message around credentials the migrated code actually supports:

Legacy:
```java
throw new IllegalArgumentException(
    "When running tests in record mode either 'AZURE_AUTH_LOCATION' or " +
    "'AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET and AZURE_SUBSCRIPTION_ID' needs to be set");
```

Migrated:
```java
throw new IllegalArgumentException(
    "Environment variables AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET, " +
    "and AZURE_SUBSCRIPTION_ID must be set");
```

#### 3d — Update imports

Remove all legacy imports; replace with:
```java
import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.management.resources.core.ResourceGroupTaggingPolicy;
```

> Remove imports for `ApplicationTokenCredentials`, `AzureResponseBuilder`, `AzureJacksonAdapter`, `RestClient`, `ProviderRegistrationInterceptor`, `ResourceGroupTaggingInterceptor`, `LogLevel`, `File`, `TimeUnit`.
> Keep `java.io.IOException` if still needed.

---

## VALIDATION & FIX STEPS

**Make sure**:

1. Migrated project passes compilation:
   ```bash
   mvn compile -DJAVA_HOME="C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7"
   ```

2. All tests pass. Don't silently skip tests:
   ```bash
   mvn test
   ```

3. The plan's `Guidelines` section records the freshly resolved latest stable BOM target exactly as `TARGET_AZURE_SDK_BOM_VERSION = 1.3.7`; this value must not remain a placeholder, must not be copied from the original project, and must match the current Azure SDK for Java BOM source of truth at validation time.

4. No legacy SDK dependencies/references exist. This is a **hard gate**, not a self-assessment — you must prove it by running the commands below from the repo root and showing they return zero hits. Do not declare migration complete until all three return empty:

   ```bash
   # 1. Legacy groupId / artifact references in ANY text file (pom.xml, *.gradle, *.gradle.kts, libs.versions.toml, Dockerfile, *.sh, *.md, etc.)
   grep -RIn --exclude-dir={.git,target,build,node_modules,out} \
     -E 'com\.microsoft\.azure(\.|:)|microsoft-azure-|azure-eventhubs-eph|azure-keyvault(:|["'"'"'"])' .

   # 2. Legacy imports still in Java sources
   grep -RIn --include='*.java' -E '^\s*import\s+com\.microsoft\.azure\.' .

   # 3. Every pom.xml and *.gradle(.kts) file in the repo (not just the root reactor) — eyeball each for legacy coordinates
   find . -type d \( -name .git -o -name target -o -name build -o -name node_modules \) -prune -o \
     -type f \( -name 'pom.xml' -o -name '*.gradle' -o -name '*.gradle.kts' -o -name 'libs.versions.toml' \) -print
   ```

   Pay special attention to files outside the root Maven reactor — e.g., `.ci/**/pom.xml`, `buildSrc/`, sample sub-modules, archetype resources — these are frequently missed because `mvn dependency:tree` on the root project never visits them.

5. If `azure-sdk-bom` is used, ensure the BOM version exactly matches `TARGET_AZURE_SDK_BOM_VERSION = 1.3.7` and there are **NO** explicit version dependencies for Azure libraries managed by the BOM. For example, instead of `<version>2.62.0</version>` on `azure-resourcemanager`, there should be no `<version>` tag at all.

6. For each migration guide recorded during migration:
   1. Fetch and read the full content of the guide URL.
   2. Identify the migrated source files that correspond to that guide's package.
   3. Verify the migrated code follows the guide's recommended API replacements, class mappings, authentication patterns, and async/sync conventions.
   4. Fix any deviations — do not just report them.
