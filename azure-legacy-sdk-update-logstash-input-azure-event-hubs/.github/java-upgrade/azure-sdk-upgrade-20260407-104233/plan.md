# Upgrade Plan: logstash-input-azure-event-hubs (azure-sdk-upgrade-20260407-104233)

- **Generated**: 2026-04-07 10:42:33
- **HEAD Branch**: main
- **HEAD Commit ID**: f171b6b130257cf01041cd7da5ca780a64adcb9d

## Available Tools

**JDKs**
- JDK 21.0.3: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home (current project JDK)

**Build Tools**
- Gradle Wrapper: 8.7 (compatible with JDK 21.0.3)

## Guidelines

- This is a Logstash plugin that uses Java Azure SDK libraries. The main plugin code is in Ruby, but it imports and uses Java classes from Azure EventHub SDK.
- The project has a main Gradle build file and two Maven subprojects under `.ci/integration/` for testing purposes.
- Preserve the Ruby code structure and only update Java dependencies and Java source files.
- For EventProcessorHost with InMemoryCheckpointManager/InMemoryLeaseManager, need to create custom InMemoryCheckpointStore based on [SampleCheckpointStore](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/eventhubs/azure-messaging-eventhubs/src/samples/java/com/azure/messaging/eventhubs/SampleCheckpointStore.java).

## Upgrade Goals

- Replace all `com.microsoft.azure.*` dependencies with `com.azure.*` equivalents
- Migrate Java source code to use modern Azure SDK APIs (builder pattern, modern EventHub client)
- Ensure all tests and builds pass after migration
- Update both the main Gradle project and the Maven CI projects

## Technology Stack

| Technology/Dependency                                  | Current  | Modern Equivalent                                           | Migration Notes                                                                                      |
| ------------------------------------------------------ | -------- | ----------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| com.microsoft.azure:azure-eventhubs                    | 3.3.0    | com.azure:azure-messaging-eventhubs                         | Use azure-sdk-bom for version management; migrate to EventHubProducerClient/EventHubConsumerClient |
| com.microsoft.azure:qpid-proton-j-extensions           | 1.2.4    | N/A (transitive dependency no longer needed)                | This is a transitive dependency of legacy azure-eventhubs, not needed with modern SDK               |
| com.microsoft.azure:azure-eventhubs-eph                | 3.3.0    | com.azure:azure-messaging-eventhubs (includes EPH support)  | Migrate to EventProcessorClient with builder pattern                                                |
| com.microsoft.azure:azure-storage                      | 8.6.6    | com.azure:azure-storage-blob                                | Use azure-sdk-bom; migrate to BlobContainerClient and related classes                               |
| Gradle Wrapper                                         | 8.7      | -                                                           | Compatible with JDK 21, no upgrade needed                                                            |
| Maven (CI subprojects)                                 | 3.7.0    | -                                                           | Via maven-compiler-plugin config, compatible with JDK 8                                              |

**Maven CI Projects:**
- `.ci/integration/event_hub_producer` - uses com.microsoft.azure:azure-eventhubs:1.0.1
- `.ci/integration/event_hub_consumer` - uses com.microsoft.azure:azure-eventhubs:1.0.1 and azure-eventhubs-eph:1.0.0

## Derived Upgrades

This section documents dependencies that must be upgraded to maintain compatibility:

| Upgrade Reason                                         | Component                           | Action                                                   |
| ------------------------------------------------------ | ----------------------------------- | -------------------------------------------------------- |
| Modern Azure SDK dependency management                 | azure-sdk-bom                       | Add com.azure:azure-sdk-bom with latest stable version   |
| EventProcessorClient checkpoint store                  | Custom InMemoryCheckpointStore      | Create custom implementation based on SampleCheckpointStore |

## Key Challenges

1. **EventProcessorHost to EventProcessorClient Migration**: The legacy code uses EventProcessorHost with InMemoryCheckpointManager and InMemoryLeaseManager. The modern SDK doesn't provide in-memory implementations, so we need to create a custom InMemoryCheckpointStore based on the sample code.

2. **Multiple Build Systems**: The project uses both Gradle (main plugin) and Maven (CI testing projects). Need to update dependencies in both systems.

3. **Ruby Java Interop**: The main Logstash plugin is written in Ruby but imports Java classes. Need to ensure the Ruby code imports are updated to match the new package names.

4. **Reflection and Internal APIs**: The consumer code uses reflection to access internal APIs (getHostContext, initialize methods). This approach may not work with modern SDK and needs to be refactored.

5. **Storage Account Dependencies**: The legacy azure-storage library needs migration to azure-storage-blob which has significantly different APIs.

## Upgrade Steps

### Step 1: Setup Baseline (MANDATORY)

**Goal**: Establish baseline by compiling and testing with current configuration

**Actions**:
1. Run `./gradlew clean compileJava compileTestJava` to verify main project compilation
2. For each Maven CI project, run `mvn clean test-compile` to verify compilation
3. Run `./gradlew test` to establish baseline test results
4. Document any pre-existing failures

**Verification**:
- Main project compiles successfully (both main and test sources)
- Maven CI projects compile successfully
- Baseline test results documented

**Expected Result**: Compilation success; tests may fail

---

### Step 2: Add Modern Azure SDK BOM to Gradle

**Goal**: Add azure-sdk-bom dependency management to the main Gradle project

**Actions**:
1. Look up latest stable azure-sdk-bom version from https://repo1.maven.org/maven2/com/azure/azure-sdk-bom/
2. Add azure-sdk-bom to dependencyManagement in build.gradle
3. No other changes in this step

**Verification**:
- `./gradlew dependencies` shows azure-sdk-bom in dependency tree
- Project still compiles

**Expected Result**: Compilation success

---

### Step 3: Create Custom InMemoryCheckpointStore

**Goal**: Create custom checkpoint store implementation for EventProcessorClient

**Actions**:
1. Create `src/main/java/com/azure/messaging/eventhubs/checkpointstore/inmemory/InMemoryCheckpointStore.java`
2. Implement based on [SampleCheckpointStore](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/eventhubs/azure-messaging-eventhubs/src/samples/java/com/azure/messaging/eventhubs/SampleCheckpointStore.java)
3. Ensure it implements the CheckpointStore interface from azure-messaging-eventhubs

**Verification**:
- File exists and compiles
- Class implements required CheckpointStore interface

**Expected Result**: Compilation success

---

### Step 4: Update Gradle Dependencies

**Goal**: Replace legacy Azure SDK dependencies with modern equivalents in build.gradle

**Actions**:
1. Replace `com.microsoft.azure:azure-eventhubs:3.3.0` with `com.azure:azure-messaging-eventhubs` (no version - managed by BOM)
2. Remove `com.microsoft.azure:qpid-proton-j-extensions:1.2.4` (no longer needed)
3. Remove `com.microsoft.azure:azure-eventhubs-eph:3.3.0` (included in azure-messaging-eventhubs)
4. Replace `com.microsoft.azure:azure-storage:8.6.6` with `com.azure:azure-storage-blob` (no version - managed by BOM)
5. Remove exclusion of `adal4j` as it's no longer a transitive dependency

**Verification**:
- `./gradlew dependencies` shows new azure-messaging-eventhubs and azure-storage-blob dependencies
- No com.microsoft.azure.* dependencies remain in the tree

**Expected Result**: Compilation will fail (expected) - source code not yet updated

---

### Step 5: Migrate Producer Java Code

**Goal**: Update event_hub_producer Java code to use modern EventHub SDK

**Actions**:
1. Update imports in `.ci/integration/event_hub_producer/src/main/java/org/logstash/Producer.java`
2. Replace `EventHubClient.createSync()` with `EventHubProducerClient` builder pattern
3. Update event sending code to use modern API
4. Update pom.xml dependencies to use azure-sdk-bom and azure-messaging-eventhubs

**Verification**:
- `cd .ci/integration/event_hub_producer && mvn clean compile` succeeds

**Expected Result**: Producer compiles successfully

---

### Step 6: Migrate Consumer Java Code

**Goal**: Update event_hub_consumer Java code to use modern EventProcessorClient

**Actions**:
1. Update package declaration (move out of com.microsoft.azure.eventprocessorhosts if needed)
2. Update imports in all consumer Java files
3. Replace EventProcessorHost with EventProcessorClientBuilder
4. Replace IEventProcessor with functional interfaces (processEvent, processError, processPartitionInitialization, processPartitionClose)
5. Use custom InMemoryCheckpointStore instead of InMemoryCheckpointManager/InMemoryLeaseManager
6. Remove reflection-based initialization code
7. Update pom.xml dependencies

**Verification**:
- `cd .ci/integration/event_hub_consumer && mvn clean compile` succeeds

**Expected Result**: Consumer compiles successfully

---

### Step 7: Update Ruby Java Imports

**Goal**: Update Ruby code to import modern Azure SDK Java classes

**Actions**:
1. Update `lib/logstash/inputs/azure_event_hubs.rb` to import from `com.azure.messaging.eventhubs` instead of `com.microsoft.azure.eventprocessorhost`
2. Update any other Ruby files that import Azure SDK classes
3. Regenerate the auto-generated jar requires file with `./gradlew generateGemJarRequiresFile`

**Verification**:
- Ruby syntax is valid
- Jar requires file updated with new dependencies

**Expected Result**: Gradle build succeeds

---

### Step 8: Build and Package

**Goal**: Verify the complete project builds and packages correctly

**Actions**:
1. Run `./gradlew clean build` to build the main project
2. Run `./gradlew vendor` to vendor dependencies
3. Build both Maven CI projects: `cd .ci/integration/event_hub_producer && mvn clean package` and `cd .ci/integration/event_hub_consumer && mvn clean package`

**Verification**:
- All builds succeed
- JAR files are created
- vendored dependencies include modern Azure SDK

**Expected Result**: All builds succeed

---

### Step 9: Final Validation (MANDATORY)

**Goal**: Verify all upgrade goals met and 100% tests pass

**Actions**:
1. Verify no legacy `com.microsoft.azure.*` dependencies remain: `./gradlew dependencies | grep com.microsoft.azure`
2. Run full test suite: `./gradlew clean test`
3. Verify all tests pass (100% pass rate required)
4. If any tests fail, debug and fix iteratively
5. Validate against [EventHubs migration guide](https://aka.ms/azsdk/java/migrate/eh)
6. Review all changes for completeness and correctness

**Verification**:
- Zero occurrences of com.microsoft.azure in dependency tree
- All tests pass
- All source files use modern Azure SDK APIs
- Migration follows the official migration guide

**Expected Result**: 100% test pass rate, zero legacy dependencies

---

## Plan Review

This plan covers:
- ✓ All legacy Azure SDK dependencies identified
- ✓ Migration path for each dependency documented
- ✓ Both Gradle and Maven build files included
- ✓ Java source code migration planned
- ✓ Ruby interop considered
- ✓ Custom checkpoint store implementation planned
- ✓ Incremental approach with isolated steps
- ✓ Validation at each step

Potential limitations:
- If the Logstash plugin runtime has specific requirements for Java class versions, additional testing may be needed
- The custom InMemoryCheckpointStore may need adjustments based on specific usage patterns in the plugin

The plan follows the incremental upgrade strategy with risk-first approach (addressing the complex EventProcessorHost migration early).
