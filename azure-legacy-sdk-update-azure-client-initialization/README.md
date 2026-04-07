# Azure Client Initialization - SDK Migration

This project demonstrates migrating from legacy Azure SDK for Java to the modern Azure SDK.

## Overview

This sample shows how to initialize Azure management clients, migrated from the legacy `com.microsoft.azure:azure` SDK to the modern `com.azure.resourcemanager:azure-resourcemanager` SDK.

## Migration Summary

### Dependencies Changed

**Before:**
```xml
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>azure</artifactId>
    <version>1.36.3</version>
</dependency>
```

**After:**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.azure</groupId>
            <artifactId>azure-sdk-bom</artifactId>
            <version>1.2.30</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    <dependency>
        <groupId>com.azure.resourcemanager</groupId>
        <artifactId>azure-resourcemanager</artifactId>
    </dependency>
    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-identity</artifactId>
    </dependency>
</dependencies>
```

### Code Changes

#### 1. Authentication

**Legacy SDK:**
```java
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;

ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
    clientId, tenantId, clientSecret, AzureEnvironment.AZURE);
credentials.withDefaultSubscriptionId(subscriptionId);
```

**Modern SDK:**
```java
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.core.management.AzureEnvironment;

TokenCredential credential = new ClientSecretCredentialBuilder()
    .clientId(clientId)
    .tenantId(tenantId)
    .clientSecret(clientSecret)
    .authorityHost(AzureEnvironment.AZURE.getActiveDirectoryEndpoint())
    .build();
```

#### 2. Client Initialization

**Legacy SDK:**
```java
import com.microsoft.azure.management.Azure;
import com.microsoft.rest.RestClient;

RestClient.Builder builder = new RestClient.Builder()
    .withBaseUrl(baseUrl)
    .withSerializerAdapter(new AzureJacksonAdapter())
    .withResponseBuilderFactory(new AzureResponseBuilder.Factory())
    .withInterceptor(new ProviderRegistrationInterceptor(credentials))
    .withNetworkInterceptor(new ResourceGroupTaggingInterceptor())
    .withCredentials(credentials)
    .withLogLevel(LogLevel.BODY_AND_HEADERS)
    .withReadTimeout(3, TimeUnit.MINUTES);

Azure.Authenticated azureAuthed = Azure.authenticate(builder.build(), subscriptionId, credentials.domain());
Azure azure = azureAuthed.withSubscription(subscriptionId);
```

**Modern SDK:**
```java
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.http.policy.HttpLogDetailLevel;

AzureProfile profile = new AzureProfile(tenantId, subscriptionId, AzureEnvironment.AZURE);

AzureResourceManager azureResourceManager = AzureResourceManager
    .configure()
    .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS))
    .withPolicy(new com.azure.core.http.policy.RetryPolicy("Retry-After", Duration.ofSeconds(60)))
    .authenticate(credential, profile)
    .withSubscription(subscriptionId);
```

### Key Migration Points

1. **Simplified Configuration**: The modern SDK handles provider registration and resource tagging internally - no need for custom interceptors
2. **BOM Management**: Using explicit versions instead of BOM for clarity in this migration example
3. **Separate Identity Library**: Authentication moved to dedicated `azure-identity` library
4. **Type-Safe Credentials**: `TokenCredential` interface provides better abstraction
5. **AzureProfile**: Encapsulates environment, tenant, and subscription configuration
6. **Fluent Configuration**: Modern SDK provides cleaner builder patterns
7. **Removed Files**: `ResourceGroupTaggingInterceptor.java` was removed as the modern SDK handles resource tagging through built-in policies and tags can be applied directly when creating resources

### Files Removed During Migration

#### ResourceGroupTaggingInterceptor.java
This legacy OkHttp interceptor was used to automatically tag resource groups during creation. With the modern SDK:
- Resource tagging is handled more elegantly through the resource builder patterns
- Tags can be applied directly: `resourceGroup.withTags(tags)` when creating resources
- Azure Policy can enforce tagging at the subscription/management group level
- No need for low-level HTTP interceptors

## Running the Sample

### Prerequisites
- Java 11 or later
- Maven 3.6+
- Azure subscription with service principal

### Environment Variables
Set the following environment variables:
```bash
export AZURE_CLIENT_ID=<your-client-id>
export AZURE_TENANT_ID=<your-tenant-id>
export AZURE_CLIENT_SECRET=<your-client-secret>
export AZURE_SUBSCRIPTION_ID=<your-subscription-id>
```

### Build and Run
```bash
mvn clean compile
mvn exec:java
```

## Additional Resources

- [Azure SDK for Java Documentation](https://docs.microsoft.com/azure/developer/java/sdk/)
- [Azure Identity Documentation](https://docs.microsoft.com/java/api/overview/azure/identity-readme)
- [Azure Resource Manager Documentation](https://docs.microsoft.com/java/api/overview/azure/resourcemanager-readme)
- [Migration Guide](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/resourcemanager/docs/MIGRATION_GUIDE.md)
