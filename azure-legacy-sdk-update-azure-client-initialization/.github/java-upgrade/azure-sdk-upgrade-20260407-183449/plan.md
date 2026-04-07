# Upgrade Plan: azure-client-initialization (azure-sdk-upgrade-20260407-183449)

- **Generated**: 2026-04-07 18:34:49
- **HEAD Branch**: policy_instructions
- **HEAD Commit ID**: f171b6b130257cf01041cd7da5ca780a64adcb9d

## Available Tools

**JDKs**
- JDK 21.0.3: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home (used by all steps)

**Build Tools**
- Maven 3.9.10: /opt/homebrew/Cellar/maven/3.9.10/libexec

## Guidelines

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Upgrade Goals

- Replace all `com.microsoft.azure.*` dependencies with `com.azure.*` equivalents
- Migrate source code to use modern Azure SDK APIs (builder pattern, Azure Identity)
- Maintain functional equivalence - keep authentication patterns, resource operations unchanged
- Add concise comments explaining non-obvious migration changes
- Ensure 100% test pass rate (or establish baseline if tests exist)

## Technology Stack

| Technology/Dependency                              | Current | Modern Equivalent                                  | Migration Notes                                           |
| -------------------------------------------------- | ------- | -------------------------------------------------- | --------------------------------------------------------- |
| com.microsoft.azure:azure                          | 1.36.3  | com.azure.resourcemanager:azure-resourcemanager    | Use azure-sdk-bom for version management                  |
| com.microsoft.azure:azure-client-authentication    | 1.7.8   | com.azure:azure-identity                           | Use ClientSecretCredential or DefaultAzureCredential      |
| com.fasterxml.jackson.core:jackson-databind        | 2.9.8   | (Explicit dependency)                              | Needed for credential file parsing, ensure compatible ver |
| Maven                                              | 3.9.10  | -                                                  | Compatible with JDK 21                                    |
| JDK                                                | 21.0.3  | -                                                  | Already JDK 8+, no upgrade needed                         |

### Derived Upgrades

- Add `com.azure:azure-sdk-bom` for centralized version management of Azure SDK dependencies
- Replace legacy authentication (`ApplicationTokenCredentials`) with modern `ClientSecretCredential` from `azure-identity`
- Ensure `jackson-databind` is explicitly included for file-based authentication credential parsing
- Migrate OkHttp interceptors (`ProviderRegistrationInterceptor`, `ResourceGroupTaggingInterceptor`) to HTTP pipeline policies (`ProviderRegistrationPolicy`, custom policy)
- According to migration guide, `Azure` (premium client) does not need `ProviderRegistrationPolicy`, so only custom `ResourceGroupTaggingPolicy` is needed

## Upgrade Steps

- **Step 1: Setup Baseline**
  - **Rationale**: Establish pre-upgrade compile and test results to measure upgrade success against.
  - **Changes to Make**:
    - [ ] Run baseline compilation with current JDK
    - [ ] Document compile results (main and test code)
  - **Verification**:
    - Command: `mvn clean test-compile`
    - JDK: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    - Expected: Document SUCCESS/FAILURE (this project has no tests)

- **Step 2: Migrate Azure SDK Dependencies**
  - **Rationale**: Replace legacy com.microsoft.azure dependencies with modern com.azure.resourcemanager equivalents to enable modern API usage.
  - **Changes to Make**:
    - [ ] Add azure-sdk-bom (version 1.3.3 or latest stable) to dependencyManagement section
    - [ ] Replace `com.microsoft.azure:azure:1.36.3` with `com.azure.resourcemanager:azure-resourcemanager` (no explicit version)
    - [ ] Add `com.azure:azure-identity` (no explicit version, managed by BOM)
    - [ ] Add explicit `com.fasterxml.jackson.core:jackson-databind` with compatible version (2.17.0 or latest)
    - [ ] Remove legacy `com.microsoft.azure:azure` dependency
  - **Verification**:
    - Command: `mvn clean test-compile`
    - JDK: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    - Expected: Compilation FAILURE expected (source code not yet migrated)

- **Step 3: Migrate Source Code to Modern Azure SDK**
  - **Rationale**: Update Java code to use modern Azure SDK APIs, replacing legacy authentication and client initialization patterns.
  - **Changes to Make**:
    - [ ] Update imports from `com.microsoft.azure.management.*` to `com.azure.resourcemanager.*`
    - [ ] Replace `ApplicationTokenCredentials` with `ClientSecretCredential` and `AzureProfile`
    - [ ] Replace `RestClient.Builder` with `AzureResourceManager.configure()` builder pattern
    - [ ] Migrate `ProviderRegistrationInterceptor` to NOT be added (Azure/premium client doesn't need it)
    - [ ] Migrate `ResourceGroupTaggingInterceptor` to custom `HttpPipelinePolicy` implementation
    - [ ] Update file-based authentication to use Jackson ObjectMapper for credential file parsing
    - [ ] Update main class reference in pom.xml exec plugin configuration
  - **Verification**:
    - Command: `mvn clean test-compile`
    - JDK: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    - Expected: Compilation SUCCESS

- **Step 4: Final Validation**
  - **Rationale**: Verify all upgrade goals met, project compiles successfully, no legacy dependencies remain.
  - **Changes to Make**:
    - [ ] Verify no legacy com.microsoft.azure.* dependencies exist
    - [ ] Verify all imports use com.azure.* packages
    - [ ] Verify no explicit versions for Azure libraries that are in azure-sdk-bom
    - [ ] Clean rebuild with current JDK
    - [ ] Review migrated code against migration guide at https://aka.ms/java-track2-migration-guide
  - **Verification**:
    - Command: `mvn clean compile && mvn dependency:tree | grep -i microsoft.azure`
    - JDK: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    - Expected: Compilation SUCCESS, no legacy dependencies found

## Key Challenges

- **Authentication Migration**
  - **Challenge**: Legacy code uses both environment variable-based authentication (ApplicationTokenCredentials) and file-based authentication (Azure.authenticate(credentialFile)). Modern SDK requires explicit credential construction.
  - **Strategy**: For env-based: construct ClientSecretCredential with tenantId, clientId, clientSecret. For file-based: read credential JSON with Jackson ObjectMapper, extract fields, construct ClientSecretCredential. Use AzureProfile for subscription context.

- **Custom Interceptors Migration**
  - **Challenge**: Legacy code uses OkHttp interceptors (ProviderRegistrationInterceptor, ResourceGroupTaggingInterceptor). Modern SDK uses HttpPipelinePolicy.
  - **Strategy**: According to migration guide, Azure (premium client) doesn't need ProviderRegistrationPolicy. For ResourceGroupTaggingInterceptor, implement custom HttpPipelinePolicy that adds tags to resource group requests. Preserve exact tagging logic.

- **API Surface Changes**
  - **Challenge**: RestClient.Builder pattern differs from AzureResourceManager.configure() pattern. Legacy uses .withXxx() methods, modern uses different builder methods.
  - **Strategy**: Follow migration guide mappings: withLogLevel(LogLevel.BODY_AND_HEADERS) → withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS)). Replace withInterceptor/withNetworkInterceptor with withPolicy.
