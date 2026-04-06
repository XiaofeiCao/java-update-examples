# Upgrade Plan: logstash-input-azure-event-hubs (azure-sdk-upgrade-20260406-211341)

- **Generated**: 2026-04-06 21:13:41
- **HEAD Branch**: logstash_case_study
- **HEAD Commit ID**: ca5312ca926dedb72a91ed5175b1cb6d394dd726

## Available Tools

**JDKs**
- JDK 17: /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home (used for compilation, compatible with Java 1.8 source/target)
- JDK 21: /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home (default, compatible with Java 1.8 source/target)

**Build Tools**
- Gradle Wrapper: 8.7 (detected in gradle/wrapper/gradle-wrapper.properties)

## Guidelines

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Upgrade Goals

- Replace all `com.microsoft.azure.*` dependencies with `com.azure.*` equivalents
- Migrate source code in integration test projects to use modern Azure SDK APIs (builder pattern, async patterns)
- Maintain compatibility with Java 1.8 source/target levels
- Ensure all integration test code compiles successfully

## Technology Stack

| Technology/Dependency                          | Current | Modern Equivalent                                 | Migration Notes                                                  |
| ---------------------------------------------- | ------- | ------------------------------------------------- | ---------------------------------------------------------------- |
| com.microsoft.azure:azure-eventhubs            | 3.3.0   | com.azure:azure-messaging-eventhubs               | Modern Event Hubs client library                                 |
| com.microsoft.azure:qpid-proton-j-extensions   | 1.2.4   | N/A (internal dependency)                         | Removed - modern SDK uses different AMQP implementation          |
| com.microsoft.azure:azure-eventhubs-eph        | 3.3.0   | com.azure:azure-messaging-eventhubs-checkpointstore-blob | Modern Event Processor with blob storage checkpoint store |
| com.microsoft.azure:azure-storage              | 8.6.6   | com.azure:azure-storage-blob                      | Modern Blob Storage client library                               |
| Gradle Wrapper                                 | 8.7     | -                                                 | Compatible with project requirements                             |
| maven-compiler-plugin (integration tests)      | 3.7.0   | -                                                 | Check if upgrade needed for compatibility                        |

### Derived Upgrades

- Add `azure-sdk-bom` for centralized version management of com.azure.* dependencies
- Replace `azure-eventhubs` with `azure-messaging-eventhubs` (modern Event Hubs library with new async patterns)
- Replace `azure-eventhubs-eph` with `azure-messaging-eventhubs-checkpointstore-blob` (modern Event Processor Host replacement)
- Replace `azure-storage:8.6.6` with `azure-storage-blob` (modern Blob Storage library)
- Update integration test Maven projects to use modern Azure SDKs for compatibility

## Upgrade Steps

- **Step 1: Setup Baseline**
  - **Rationale**: Establish pre-upgrade compile and test results to measure upgrade success against.
  - **Changes to Make**:
    - [ ] Run baseline compilation for main build.gradle with JDK 21
    - [ ] Run baseline compilation for integration test Maven projects
    - [ ] Document current state (SUCCESS/FAILURE)
  - **Verification**:
    - Command: `./gradlew clean compileJava && cd .ci/integration/event_hub_producer && mvn clean compile -q && cd ../event_hub_consumer && mvn clean compile -q`
    - JDK: JDK 21 (/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home)
    - Expected: Document SUCCESS/FAILURE for each component

---

- **Step 2: Migrate Main Gradle Dependencies**
  - **Rationale**: Replace legacy com.microsoft.azure Event Hubs and Storage dependencies with modern com.azure equivalents in the main build.gradle.
  - **Changes to Make**:
    - [ ] Add azure-sdk-bom to dependency management in build.gradle
    - [ ] Replace com.microsoft.azure:azure-eventhubs:3.3.0 with com.azure:azure-messaging-eventhubs
    - [ ] Replace com.microsoft.azure:azure-eventhubs-eph:3.3.0 with com.azure:azure-messaging-eventhubs-checkpointstore-blob
    - [ ] Replace com.microsoft.azure:azure-storage:8.6.6 with com.azure:azure-storage-blob
    - [ ] Remove com.microsoft.azure:qpid-proton-j-extensions (internal dependency, no longer needed)
  - **Verification**:
    - Command: `./gradlew clean compileJava`
    - JDK: JDK 21
    - Expected: Compilation SUCCESS (no Java source files to compile in main project, but dependencies should resolve)

---

- **Step 3: Migrate Event Hub Producer Maven Project**
  - **Rationale**: Update integration test producer to use modern azure-messaging-eventhubs SDK.
  - **Changes to Make**:
    - [ ] Add azure-sdk-bom to dependencyManagement in event_hub_producer/pom.xml
    - [ ] Replace com.microsoft.azure:azure-eventhubs:1.0.1 with com.azure:azure-messaging-eventhubs
    - [ ] Update Producer.java to use modern EventHubProducerClient and EventData APIs
    - [ ] Update async patterns from ExecutorService-based to CompletableFuture-based
  - **Verification**:
    - Command: `cd .ci/integration/event_hub_producer && mvn clean compile -q`
    - JDK: JDK 21
    - Expected: Compilation SUCCESS

---

- **Step 4: Migrate Event Hub Consumer Maven Project**
  - **Rationale**: Update integration test consumer to use modern Event Processor with blob checkpoint store.
  - **Changes to Make**:
    - [ ] Add azure-sdk-bom to dependencyManagement in event_hub_consumer/pom.xml
    - [ ] Replace com.microsoft.azure:azure-eventhubs:1.0.1 with com.azure:azure-messaging-eventhubs
    - [ ] Replace com.microsoft.azure:azure-eventhubs-eph:1.0.0 with com.azure:azure-messaging-eventhubs-checkpointstore-blob
    - [ ] Update Consumer.java to use modern EventProcessorClient and BlobCheckpointStore
    - [ ] Update EventProcessor.java to implement modern EventProcessor interface (onEvent instead of onEvents)
    - [ ] Update InMemoryCheckpointManager/InMemoryLeaseManager usage to modern equivalents
  - **Verification**:
    - Command: `cd .ci/integration/event_hub_consumer && mvn clean compile -q`
    - JDK: JDK 21
    - Expected: Compilation SUCCESS

---

- **Step 5: Final Validation**
  - **Rationale**: Verify all upgrade goals met, project compiles successfully across all modules.
  - **Changes to Make**:
    - [ ] Verify no legacy com.microsoft.azure.* dependencies remain in build.gradle or pom.xml files
    - [ ] Resolve ALL TODOs and temporary workarounds from previous steps
    - [ ] Clean rebuild of all projects (Gradle + Maven integration tests)
    - [ ] Fix any remaining compilation errors
    - [ ] Verify migration guide compliance for Event Hubs migration
  - **Verification**:
    - Command: `./gradlew clean compileJava && cd .ci/integration/event_hub_producer && mvn clean compile -q && cd ../event_hub_consumer && mvn clean compile -q`
    - JDK: JDK 21
    - Expected: Compilation SUCCESS for all modules

## Key Challenges

- **Event Hubs API Surface Changes**
  - **Challenge**: Legacy azure-eventhubs uses ExecutorService-based async with EventHubClient.createSync(). Modern SDK uses CompletableFuture-based async with EventHubProducerClient built via builder pattern.
  - **Strategy**: Follow migration guide at https://aka.ms/azsdk/java/migrate/eh. Use EventHubClientBuilder for client creation, replace sendSync() with send() methods, update async patterns accordingly.

- **Event Processor Host Migration**
  - **Challenge**: Legacy EventProcessorHost with InMemoryCheckpointManager/InMemoryLeaseManager needs complete rewrite. Modern SDK uses EventProcessorClient with BlobCheckpointStore, and the in-memory approach is no longer directly supported.
  - **Strategy**: For Consumer.java, if in-memory checkpoint is required, implement custom CheckpointStore interface. Otherwise, use blob-based checkpoint store. Update EventProcessor to implement modern interface (onEvent for single event processing).

- **Storage SDK v8 to v12 Migration**
  - **Challenge**: azure-storage:8.6.6 uses completely different API surface from azure-storage-blob:12.x. However, storage is only a transitive dependency through azure-eventhubs-eph.
  - **Strategy**: Confirm if direct storage API usage exists in code. If not, remove explicit dependency and let it be managed via azure-messaging-eventhubs-checkpointstore-blob.

- **No Unit Tests in Main Project**
  - **Challenge**: Main build.gradle has no Java source files or tests - it only manages dependencies for the Ruby Logstash plugin.
  - **Strategy**: Focus validation on dependency resolution and integration test compilation. The vendor task generates jar dependencies for the Ruby plugin, ensure no legacy Azure jars remain after migration.
