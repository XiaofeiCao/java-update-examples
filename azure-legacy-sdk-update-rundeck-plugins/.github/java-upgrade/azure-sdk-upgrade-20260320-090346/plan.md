# Upgrade Plan: azure-legacy-sdk-update-rundeck-plugins (azure-sdk-upgrade-20260320-090346)

- **Generated**: 2026-03-20 09:03:46 UTC
- **HEAD Branch**: test_instructions
- **HEAD Commit ID**: 7eac73ad6184c47fad4cc0017f0c6b089a644c63

## Available Tools

**JDKs**
- JDK 17.0.17: Temurin-17.0.17+10 (default, used by all steps)

**Build Tools**
- Gradle Wrapper: 7.6.6 (via `gradlew.bat`)

## Guidelines

- Migrate `com.microsoft.azure:azure:1.41.4` → `com.azure.resourcemanager:azure-resourcemanager`
- Migrate `com.microsoft.azure:azure-storage:8.6.6` → `com.azure:azure-storage-blob`
- Remove `com.microsoft.azure:azure-keyvault-core:1.0.0` (not used in code)
- Use `azure-sdk-bom:1.3.5` for centralized version management
- Follow migration guide at https://aka.ms/java-track2-migration-guide for resource manager
- Follow Storage V8→V12 migration guide for blob storage
- `com.microsoft.azure.management.**` package → `com.azure.resourcemanager.**` / `com.azure.core.management.**`
- `ApplicationTokenCredentials` → `ClientSecretCredential` / `ClientCertificateCredential` from `azure-identity`
- `Azure` class → `AzureResourceManager`
- `CloudException` → `ManagementException`
- Storage: `CloudStorageAccount` pattern → `BlobServiceClientBuilder` pattern
- `Region` from `com.microsoft.azure.management.resources.fluentcore.arm` → `com.azure.core.management.Region`
- `AzureEnvironment` from `com.microsoft.azure` → `com.azure.core.management.AzureEnvironment`
- Azure (AzureResourceManager) is a premium client — do NOT add `ProviderRegistrationPolicy`
- Source code is Groovy; preserve package declarations
- Keep all logging and stdout/stderr text unchanged
- Do not change Java source compatibility (11)

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Upgrade Goals

- Replace all `com.microsoft.azure.*` dependencies with `com.azure.*` equivalents
- Migrate source code to use modern Azure SDK APIs (builder pattern, Azure Identity)
- Migrate Azure Storage v8 to v12 (blob operations)
- Ensure 100% test pass rate

### Technology Stack

| Technology/Dependency | Current | Modern Equivalent | Migration Notes |
| --------------------- | ------- | ----------------- | --------------- |
| com.microsoft.azure:azure | 1.41.4 | com.azure.resourcemanager:azure-resourcemanager | Use azure-sdk-bom; Azure → AzureResourceManager |
| com.microsoft.azure:azure-storage | 8.6.6 | com.azure:azure-storage-blob | Use azure-sdk-bom; CloudStorageAccount → BlobServiceClient |
| com.microsoft.azure:azure-keyvault-core | 1.0.0 | Remove | Not used in source code |
| (new) com.azure:azure-sdk-bom | N/A | 1.3.5 | Centralized version management |
| (new) com.azure:azure-identity | N/A | managed by BOM | Modern authentication library |
| Gradle Wrapper | 7.6.6 | 7.6.6 | No upgrade needed |
| Java source compatibility | 11 | 11 | No change |
| org.codehaus.groovy:groovy-all | 3.0.24 | 3.0.24 | No change |
| org.rundeck:rundeck-core | 5.15.0-rc1 | 5.15.0-rc1 | No change |

### Derived Upgrades

- Add `com.azure:azure-sdk-bom:1.3.5` as enforcedPlatform for centralized version management
- Add `com.azure:azure-identity` (managed by BOM) to replace `ApplicationTokenCredentials` authentication
- Add `com.azure:azure-storage-blob` (managed by BOM) to replace legacy `azure-storage:8.6.6`
- Add `com.azure.resourcemanager:azure-resourcemanager` (managed by BOM) to replace legacy `azure:1.41.4`
- Remove CVE-related constraints for retrofit, nimbus-jose-jwt, oauth2-oidc-sdk, json-smart, okhttp (no longer transitive deps of legacy azure SDK)
- Remove `com.microsoft.azure:azure-keyvault-core` (unused)

## Upgrade Steps

- Step 1: Setup Baseline
  - **Rationale**: Establish pre-upgrade compile and test results to measure upgrade success against.
  - **Changes to Make**:
    - [ ] Run baseline compilation with current JDK
    - [ ] Run baseline tests with current JDK
  - **Verification**:
    - Command: `.\gradlew.bat clean compileTestGroovy -q && .\gradlew.bat clean test`
    - JDK: JDK 17.0.17
    - Expected: Document SUCCESS/FAILURE, test pass rate (forms acceptance criteria)

---

- Step 2: Migrate Build Configuration
  - **Rationale**: Replace legacy Azure SDK dependencies with modern equivalents in Gradle build files. This must happen first before source code migration.
  - **Changes to Make**:
    - [ ] Update `gradle/libs.versions.toml`: Replace legacy azure SDK entries with modern ones (azure-sdk-bom, azure-identity, azure-storage-blob, azure-resourcemanager)
    - [ ] Update `build.gradle`: Replace legacy pluginLibs/implementation deps with modern equivalents using enforcedPlatform BOM
    - [ ] Remove CVE-related constraints that apply only to legacy Azure SDK transitive dependencies
    - [ ] Remove json-smart force resolution (no longer needed without legacy SDK)
  - **Verification**:
    - Command: `.\gradlew.bat clean compileGroovy -q`
    - JDK: JDK 17.0.17
    - Expected: Compilation FAILURE (expected — source code still references legacy APIs)

---

- Step 3: Migrate Azure Management Source Code
  - **Rationale**: Update all source files that use Azure Resource Management APIs — authentication, VM operations, image types, regions, exceptions.
  - **Changes to Make**:
    - [ ] Migrate `AzureManager.groovy`: `ApplicationTokenCredentials` → `ClientSecretCredential`/`ClientCertificateCredential`, `Azure` → `AzureResourceManager`, `CloudException` → `ManagementException`
    - [ ] Migrate `AzurePluginUtil.groovy`: Update VM-related imports (`DataDisk`, `VirtualMachine`, `VirtualMachineExtension`)
    - [ ] Migrate `AzureNode.groovy`: Update imports for `VirtualMachine`, `VirtualMachineSize`
    - [ ] Migrate `AzureVmImageType.groovy`: Update `KnownLinuxVirtualMachineImage`, `KnownWindowsVirtualMachineImage` imports
    - [ ] Migrate `AzureVMSizeType.groovy`: Update `VirtualMachineSizeTypes` import
    - [ ] Migrate `AzureImage.groovy`: Update `ImageReference` import
    - [ ] Migrate `AzureVm.groovy`: Update `Region` import
    - [ ] Migrate all plugin files: Update VM-related imports in `AzureVMCreatePlugin`, `AzureVmStartPlugin`, `AzureVmStopPlugin`, `AzureVmListPlugin`, `AzureResourceModelSource`
  - **Verification**:
    - Command: `.\gradlew.bat clean compileGroovy -q`
    - JDK: JDK 17.0.17
    - Expected: Compilation may still fail (Storage code not yet migrated)

---

- Step 4: Migrate Azure Storage Source Code
  - **Rationale**: Replace all legacy Azure Storage v8 APIs with modern Azure Storage Blob v12 APIs.
  - **Changes to Make**:
    - [ ] Migrate `AzureFileStoragePlugin.groovy`: `CloudStorageAccount`/`CloudBlobClient`/`CloudBlobContainer`/`CloudBlockBlob` → `BlobServiceClient`/`BlobContainerClient`/`BlobClient`
    - [ ] Migrate `AzureStorageDeleteStepPlugin.groovy`: Update storage operations
    - [ ] Migrate `AzureStorageListStepPlugin.groovy`: Update listing with `BlobContainerClient.listBlobs()`, handle `CloudBlobDirectory` removal
    - [ ] Migrate `AzureEndpoint.groovy`: Update blob upload/download to v12 APIs
    - [ ] Migrate `AzureStorageCopyStepPlugin.groovy`: Update any direct storage references
  - **Verification**:
    - Command: `.\gradlew.bat clean compileTestGroovy -q`
    - JDK: JDK 17.0.17
    - Expected: Compilation SUCCESS for main code; test code may fail (mocks not yet updated)

---

- Step 5: Migrate Test Code
  - **Rationale**: Update Spock test specifications to use modern Azure SDK classes for mocking and assertions.
  - **Changes to Make**:
    - [ ] Migrate all test specs: Update Azure SDK imports and mock types
    - [ ] Update `AzureResourceModelSourceSpec.groovy`, `AzureVMCreatePluginSpec.groovy`, `AzureVmStartPluginSpec.groovy`, `AzureVmStopPluginSpec.groovy`, `AzureVmListPluginSpec.groovy`
    - [ ] Update `AzureFileStoragePluginSpec.groovy`, `AzureStorageCopyStepPluginSpec.groovy`: Update storage mock types
  - **Verification**:
    - Command: `.\gradlew.bat clean compileTestGroovy -q`
    - JDK: JDK 17.0.17
    - Expected: Compilation SUCCESS (both main and test)

---

- Step 6: Final Validation
  - **Rationale**: Verify all upgrade goals met, project compiles successfully, all tests pass.
  - **Changes to Make**:
    - [ ] Verify no legacy `com.microsoft.azure.*` dependencies remain
    - [ ] Resolve ALL TODOs and temporary workarounds from previous steps
    - [ ] Clean rebuild with current JDK
    - [ ] Fix any remaining compilation errors
    - [ ] Run full test suite and fix ALL test failures (iterative fix loop until 100% pass)
    - [ ] Verify azure-sdk-bom manages all Azure library versions (no explicit versions for BOM-managed libs)
  - **Verification**:
    - Command: `.\gradlew.bat clean test`
    - JDK: JDK 17.0.17
    - Expected: Compilation SUCCESS + 100% tests pass

## Key Challenges

- **Authentication Migration**
  - **Challenge**: Legacy code uses `ApplicationTokenCredentials` for both key-based and PFX certificate authentication. Modern SDK uses separate credential builders.
  - **Strategy**: Key auth → `ClientSecretCredentialBuilder`; PFX cert auth → `ClientCertificateCredentialBuilder`. Need to handle PFX byte array by writing to temp file if needed.

- **Storage API Architecture Change**
  - **Challenge**: Legacy Storage v8 uses `CloudStorageAccount` → `CloudBlobClient` → `CloudBlobContainer` → `CloudBlockBlob` chain. Modern v12 uses builder-based `BlobServiceClient` → `BlobContainerClient` → `BlobClient`.
  - **Strategy**: Follow V8→V12 migration guide. Replace connection-string-based initialization with `BlobServiceClientBuilder.connectionString()`.

- **CloudBlobDirectory Removal**
  - **Challenge**: `AzureStorageListStepPlugin` uses `CloudBlobDirectory` for recursive listing. Modern SDK has no direct equivalent.
  - **Strategy**: Use `BlobContainerClient.listBlobsByHierarchy()` which returns `BlobItem` with `isPrefix()` for virtual directories.

- **Groovy Dynamic Typing**
  - **Challenge**: Groovy's dynamic typing may mask some type mismatches at compile time. Test execution is critical.
  - **Strategy**: Ensure all test specs pass, which validates runtime type correctness.

- **VM Image Enum Changes**
  - **Challenge**: `KnownLinuxVirtualMachineImage` and `KnownWindowsVirtualMachineImage` may have different enum values in modern SDK.
  - **Strategy**: Verify enum values exist in modern SDK; update if names changed.

## Plan Review

- All legacy Azure SDK dependencies identified and mapped to modern equivalents
- azure-keyvault-core confirmed unused in source code — safe to remove
- CVE constraints are all for transitive deps of the legacy SDK — safe to remove with SDK migration
- azure-sdk-bom 1.3.5 confirmed to include all needed libraries (azure-identity, azure-storage-blob, azure-resourcemanager)
