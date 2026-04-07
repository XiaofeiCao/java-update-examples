# Upgrade Plan: batch-java-manage-batch-accounts (azure-sdk-upgrade-20260407-183023)

- **Generated**: 2026-04-07 18:30:23
- **HEAD Branch**: batch_instructions
- **HEAD Commit ID**: f171b6b130257cf01041cd7da5ca780a64adcb9d

## Available Tools

**JDKs**
- JDK 21.0.3: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home (used by all steps)

**Build Tools**
- Maven 3.9.10: /opt/homebrew/Cellar/maven/3.9.10/libexec

## Guidelines

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

- BatchManager (non-premium client) should be initialized with ProviderRegistrationPolicy when migrating from code using ProviderRegistrationInterceptor
- BatchAccount migration: `withNewStorageAccount` should be replaced by creating storage account separately and using `.withAutoStorage(new AutoStorageBaseProperties().withStorageAccountId(storageAccount.id()))`
- Applications and application packages should be created separately using BatchManager.applications() and BatchManager.applicationPackages() after batch account creation

## Upgrade Goals

- Replace all `com.microsoft.azure.*` dependencies with `com.azure.*` equivalents
- Migrate source code to use modern Azure SDK APIs (builder pattern, Azure Identity)
- Maintain functional equivalence with the original implementation
- Ensure 100% test pass rate (or maintain baseline if tests exist)

## Technology Stack

| Technology/Dependency | Current | Modern Equivalent | Migration Notes |
| --------------------- | ------- | ----------------- | --------------- |
| com.microsoft.azure:azure | 1.36.3 | com.azure.resourcemanager:azure-resourcemanager | Use azure-sdk-bom for version management |
| com.microsoft.azure:azure-client-authentication (implicit) | 1.7.x | com.azure:azure-identity | Replace file-based authentication with ClientSecretCredential |
| Maven | 3.9.10 | - | Compatible with JDK 21 |
| maven-compiler-plugin | 3.0 | 3.11+ | Upgrade recommended for better JDK 8+ support |
| commons-net | 3.3 | - | Keep as-is (not Azure related) |
| commons-lang | 2.6 | - | Keep as-is (not Azure related) |
| commons-lang3 | 3.7 | - | Keep as-is (not Azure related) |

## Derived Upgrades

- Add azure-sdk-bom (latest stable version 1.3.0+) for centralized version management of com.azure.* dependencies
- Replace implicit azure-client-authentication with azure-identity (modern authentication library)
- Add jackson-databind (if not already present) for parsing credential files in file-based authentication migration
- Upgrade maven-compiler-plugin to 3.11+ for better Java 8+ compatibility and build reliability

## Upgrade Steps

- **Step 1: Setup Baseline**
  - **Rationale**: Establish pre-upgrade compile and test results to measure upgrade success against.
  - **Changes to Make**:
    - [ ] Run baseline compilation with JDK 21
    - [ ] Check if tests exist and run baseline tests with JDK 21
  - **Verification**:
    - Command: `mvn clean compile test-compile -q && mvn clean test -q`
    - JDK: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    - Expected: Document SUCCESS/FAILURE, test pass rate (forms acceptance criteria)

- **Step 2: Migrate Build Configuration (pom.xml)**
  - **Rationale**: Replace legacy Azure SDK dependencies with modern equivalents and add necessary build improvements before source code migration.
  - **Changes to Make**:
    - [ ] Add azure-sdk-bom to dependencyManagement section
    - [ ] Replace com.microsoft.azure:azure with com.azure.resourcemanager:azure-resourcemanager (no version, managed by BOM)
    - [ ] Add com.azure:azure-identity (no version, managed by BOM)
    - [ ] Add com.fasterxml.jackson.core:jackson-databind (check if needed for file auth parsing)
    - [ ] Upgrade maven-compiler-plugin from 3.0 to 3.11.0
  - **Verification**:
    - Command: `mvn clean compile -q`
    - JDK: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    - Expected: Compilation FAILURE expected (legacy imports not yet migrated), dependency resolution SUCCESS

- **Step 3: Migrate Utils.java**
  - **Rationale**: Migrate utility class first as it has no authentication logic and is used by main class.
  - **Changes to Make**:
    - [ ] Update imports from com.microsoft.azure.management.batch.* to com.azure.resourcemanager.batch.models.*
    - [ ] Update imports from com.microsoft.azure.management.storage.* to com.azure.resourcemanager.storage.models.*
    - [ ] Review API changes for BatchAccount, BatchAccountKeys, Application, ApplicationPackage, StorageAccountKey
    - [ ] Keep functional behavior identical
  - **Verification**:
    - Command: `mvn clean compile -q`
    - JDK: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    - Expected: Utils.java compilation SUCCESS, ManageBatchAccount.java compilation FAILURE (not yet migrated)

- **Step 4: Migrate ManageBatchAccount.java Authentication and Imports**
  - **Rationale**: Update authentication pattern and imports before migrating batch operation code.
  - **Changes to Make**:
    - [ ] Update all imports from com.microsoft.azure.management.* to com.azure.resourcemanager.*
    - [ ] Replace Azure with AzureResourceManager
    - [ ] Implement file-based authentication using Jackson ObjectMapper + ClientSecretCredential + AzureProfile
    - [ ] Update LogLevel.BASIC to HttpLogDetailLevel.BASIC
    - [ ] Update authentication flow in main() method
  - **Verification**:
    - Command: `mvn clean compile -q`
    - JDK: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    - Expected: Partial compilation SUCCESS (authentication works, batch operations may need fixes)

- **Step 5: Migrate Batch Account Operations**
  - **Rationale**: Update batch account creation, application, and storage account management to use modern SDK patterns.
  - **Changes to Make**:
    - [ ] Replace azure.batchAccounts() with BatchManager initialization
    - [ ] Migrate batch account creation: replace withNewStorageAccount with separate storage account creation + withAutoStorage
    - [ ] Migrate application and application package creation using BatchManager.applications() and applicationPackages()
    - [ ] Update all other batch account operations (list, get, update, delete, refresh, regenerateKeys)
    - [ ] Ensure StorageManager is initialized for storage account operations
  - **Verification**:
    - Command: `mvn clean test-compile -q`
    - JDK: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    - Expected: Compilation SUCCESS (both main and test code)

- **Step 6: Final Validation**
  - **Rationale**: Verify all upgrade goals met, project compiles successfully, all tests pass.
  - **Changes to Make**:
    - [ ] Verify no legacy com.microsoft.azure.* dependencies remain in pom.xml
    - [ ] Verify no legacy imports in source code
    - [ ] Resolve ALL TODOs and temporary workarounds from previous steps
    - [ ] Clean rebuild with JDK 21
    - [ ] Fix any remaining compilation errors
    - [ ] Run full test suite and fix ALL test failures (iterative fix loop until 100% pass or match baseline)
  - **Verification**:
    - Command: `mvn clean test -q`
    - JDK: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
    - Expected: Compilation SUCCESS + 100% tests pass (or match baseline pass rate)

## Key Challenges

- **File-Based Authentication Migration**
  - **Challenge**: Legacy code uses Azure.authenticate(credFile) for file-based auth. Modern SDK requires explicit credential construction from file contents.
  - **Strategy**: Parse credential JSON file using Jackson ObjectMapper, extract clientId/clientSecret/tenantId/subscriptionId, construct ClientSecretCredential, use AzureProfile for subscription context. Handle IOException appropriately.

- **BatchAccount API Surface Changes**
  - **Challenge**: azure-resourcemanager-batch is no longer premium/handwritten. BatchAccount.withNewStorageAccount() is not available; must create storage separately and use withAutoStorage(). Applications and packages must be created via separate managers.
  - **Strategy**: Follow migration guide at https://aka.ms/java-track2-migration-guide. Create StorageAccount first using StorageManager, then create BatchAccount with withAutoStorage(new AutoStorageBaseProperties().withStorageAccountId(storageAccount.id())). Create applications and packages using batchManager.applications() and batchManager.applicationPackages() after account creation.

- **Manager Initialization Pattern**
  - **Challenge**: Legacy code uses a single Azure object for all resource management. Modern SDK requires separate managers (AzureResourceManager, BatchManager, StorageManager) with different initialization patterns.
  - **Strategy**: Initialize AzureResourceManager for resource group operations, BatchManager with ProviderRegistrationPolicy for batch operations (non-premium client), and StorageManager for storage operations. Share credential and profile across all managers.

- **Package Structure Changes**
  - **Challenge**: Package names changed from com.microsoft.azure.management.batch.* to com.azure.resourcemanager.batch.models.* and API methods may have different names/signatures.
  - **Strategy**: Use IDE refactoring and consult migration guide for method-level mappings. Verify each operation maintains functional equivalence with legacy code.
