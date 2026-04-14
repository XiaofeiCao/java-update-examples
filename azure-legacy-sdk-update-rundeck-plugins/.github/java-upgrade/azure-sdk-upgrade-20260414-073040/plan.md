# Upgrade Plan: azure-plugin (azure-sdk-upgrade-20260414-073040)

- **Generated**: 2026-04-14T07:30:40Z
- **HEAD Branch**: bom_4.6
- **HEAD Commit ID**: bb8c5312

## Available Tools

**JDKs**
- JDK 17.0.17: C:\Users\xiaofeicao\.jdks\jdk-21.0.6+7 (default via JAVA_HOME, used by all steps)

**Build Tools**
- Gradle Wrapper: 7.6.6 (via gradlew.bat)

## Guidelines

- Migrate from `com.microsoft.azure:azure` (management) to `com.azure.resourcemanager:azure-resourcemanager`
- Migrate from `com.microsoft.azure:azure-storage` (v8) to `com.azure:azure-storage-blob` (v12)
- Remove `com.microsoft.azure:azure-keyvault-core` (unused directly)
- Use `com.azure:azure-sdk-bom:1.3.6` for centralized version management
- Use `com.azure:azure-identity` for authentication (replacing `ApplicationTokenCredentials`)
- Preserve existing Groovy code patterns and functional behavior
- `Azure` is a premium client — do NOT add `ProviderRegistrationPolicy`
- Storage migration guide: V8 to V12

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Upgrade Goals

- Replace all `com.microsoft.azure.*` dependencies with `com.azure.*` equivalents
- Migrate source code to use modern Azure SDK APIs (builder pattern, Azure Identity)

### Technology Stack

| Technology/Dependency | Current | Modern Equivalent | Migration Notes |
| --------------------- | ------- | ----------------- | --------------- |
| com.microsoft.azure:azure | 1.41.4 | com.azure.resourcemanager:azure-resourcemanager | Managed by azure-sdk-bom |
| com.microsoft.azure:azure-storage | 8.6.6 | com.azure:azure-storage-blob | Managed by azure-sdk-bom |
| com.microsoft.azure:azure-keyvault-core | 1.0.0 | (Remove) | No direct usage in source code |
| N/A | N/A | com.azure:azure-identity | Required for ClientSecretCredential |
| N/A | N/A | com.azure:azure-sdk-bom:1.3.6 | Centralized version management |
| Gradle (wrapper) | 7.6.6 | - | Compatible with JDK 17 |
| groovy | 3.0.24 | - | No change needed |
| org.rundeck:rundeck-core | 5.15.0-rc1-20250821 | - | No change needed |

### Derived Upgrades

- Add `com.azure:azure-sdk-bom:1.3.6` for centralized version management
- Add `com.azure:azure-identity` for modern authentication (replaces `ApplicationTokenCredentials` from azure-client-runtime)
- Replace `com.microsoft.azure:azure` with `com.azure.resourcemanager:azure-resourcemanager`
- Replace `com.microsoft.azure:azure-storage` with `com.azure:azure-storage-blob`
- Remove `com.microsoft.azure:azure-keyvault-core` (unused directly; modern storage SDK handles its own encryption)

## Upgrade Steps

- **Step 1: Setup Baseline**
  - **Rationale**: Establish pre-upgrade compile and test results.
  - **Changes to Make**:
    - [ ] Run baseline compilation
    - [ ] Run baseline tests
  - **Verification**:
    - Command: `.\gradlew.bat clean compileTestGroovy -q && .\gradlew.bat clean test -q`
    - JDK: JAVA_HOME (JDK 21)
    - Expected: Document SUCCESS/FAILURE, test pass rate

- **Step 2: Migrate Dependencies**
  - **Rationale**: Replace legacy Azure SDK dependencies with modern equivalents in build configuration.
  - **Changes to Make**:
    - [ ] Update `gradle/libs.versions.toml`: add azure-sdk-bom, azure-identity, azure-resourcemanager, azure-storage-blob; remove legacy azure, azure-storage, azure-keyvault-core
    - [ ] Update `build.gradle`: replace legacy dependency references with modern ones, add azure-sdk-bom as enforcedPlatform, remove security CVE constraints that are no longer needed for legacy deps
  - **Verification**:
    - Command: `.\gradlew.bat clean compileTestGroovy`
    - JDK: JAVA_HOME
    - Expected: Compilation will FAIL (expected, source code not yet migrated)

- **Step 3: Migrate Azure Management SDK Source Code**
  - **Rationale**: Update all source files that use `com.microsoft.azure.management.*` to use `com.azure.resourcemanager.*` APIs.
  - **Changes to Make**:
    - [ ] Migrate `AzureManager.groovy`: Replace Azure→AzureResourceManager, ApplicationTokenCredentials→ClientSecretCredential, CloudException→ManagementException, update VM operations
    - [ ] Migrate `AzureManagerBuilder.groovy`: Region import update
    - [ ] Migrate `AzureNode.groovy`, `AzureVm.groovy`, `AzureImage.groovy`, `AzureVmImageType.groovy`, `AzureVMSizeType.groovy`: Update imports and API usage
    - [ ] Migrate `AzurePluginUtil.groovy`: Update VirtualMachine, DataDisk, VirtualMachineExtension imports
    - [ ] Migrate plugin files: `AzureVMCreatePlugin.groovy`, `AzureVmListPlugin.groovy`, `AzureFileStoragePlugin.groovy` (remove unused HasManager import)
  - **Verification**:
    - Command: `.\gradlew.bat clean compileTestGroovy`
    - JDK: JAVA_HOME
    - Expected: Compilation SUCCESS for management-related code (storage code may still fail)

- **Step 4: Migrate Azure Storage SDK Source Code**
  - **Rationale**: Migrate from Azure Storage SDK v8 (`CloudStorageAccount`, `CloudBlobClient`) to v12 (`BlobServiceClientBuilder`, `BlobContainerClient`, `BlobClient`).
  - **Changes to Make**:
    - [ ] Migrate `AzureFileStoragePlugin.groovy`: CloudStorageAccount→BlobServiceClientBuilder, CloudBlobClient→BlobServiceClient, CloudBlobContainer→BlobContainerClient, CloudBlockBlob→BlobClient
    - [ ] Migrate `AzureStorageDeleteStepPlugin.groovy`: Same storage migration pattern
    - [ ] Migrate `AzureStorageListStepPlugin.groovy`: Same pattern + CloudBlobDirectory→BlobItem with hierarchy listing
    - [ ] Migrate `AzureEndpoint.groovy`: Same storage migration pattern
  - **Verification**:
    - Command: `.\gradlew.bat clean compileTestGroovy`
    - JDK: JAVA_HOME
    - Expected: Compilation SUCCESS

- **Step 5: Final Validation**
  - **Rationale**: Verify all upgrade goals met, all tests pass.
  - **Changes to Make**:
    - [ ] Update test files: `AzureResourceModelSourceSpec.groovy`, `AzureVmListPluginSpec.groovy`, `AzureFileStoragePluginSpec.groovy` to use modern SDK types
    - [ ] Verify no legacy `com.microsoft.azure.*` dependencies remain
    - [ ] Resolve ALL TODOs
    - [ ] Run full test suite and fix ALL test failures
  - **Verification**:
    - Command: `.\gradlew.bat clean test`
    - JDK: JAVA_HOME
    - Expected: Compilation SUCCESS + 100% tests pass

## Key Challenges

- **Authentication Migration**
  - **Challenge**: Legacy `ApplicationTokenCredentials` is replaced by `ClientSecretCredential` from azure-identity. The code supports both key-based and certificate-based auth.
  - **Strategy**: Use `ClientSecretCredentialBuilder` for key-based auth. For certificate-based auth, use `ClientCertificateCredentialBuilder`.

- **Storage SDK v8→v12 API Paradigm Shift**
  - **Challenge**: v8 uses `CloudStorageAccount.parse(connectionString)` pattern with `CloudBlobClient/CloudBlobContainer/CloudBlockBlob`. v12 uses builder-based `BlobServiceClientBuilder` with `BlobContainerClient/BlobClient`.
  - **Strategy**: Follow the Storage v8→v12 migration guide. Map each v8 class to its v12 equivalent.

- **Groovy Source Code**
  - **Challenge**: All source is Groovy, not Java. Need to ensure modern SDK patterns work correctly in Groovy syntax.
  - **Strategy**: Carefully test Groovy-specific patterns (closures, safe navigation, dynamic typing).

- **`SdkContext.randomResourceName` Removal**
  - **Challenge**: Modern SDK does not have `SdkContext.randomResourceName`. Need alternative.
  - **Strategy**: Use `AzureResourceManager.sdkContext().randomResourceName()` or implement a simple utility.

- **Test Mocks**
  - **Challenge**: Tests use `GroovyMock(Azure)` which must change to `GroovyMock(AzureResourceManager)`.
  - **Strategy**: Update mock types while preserving test behavior.

- **Migration guide URLs**:
  - Management: https://aka.ms/java-track2-migration-guide
  - Storage v8→v12: https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/sdk/storage/azure-storage-blob/migrationGuides/V8_V12.md
