/**
 * Modern Azure SDK initialization using azure-resourcemanager and azure-identity
 * Migrated from: https://github.com/Azure/azure-libraries-for-java/blob/b324fabb9ba2c9687614c800c6ae69e189ce990e/azure-mgmt-resources/src/test/java/com/microsoft/azure/management/resources/core/TestBase.java
 */
package com.microsoft.azure.management;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;

public class AzureInitialization {
    public static void main(String[] args) {
        String clientId = System.getenv("AZURE_CLIENT_ID");
        String tenantId = System.getenv("AZURE_TENANT_ID");
        String clientSecret = System.getenv("AZURE_CLIENT_SECRET");
        String subscriptionId = System.getenv("AZURE_SUBSCRIPTION_ID");
        if (clientId == null || tenantId == null || clientSecret == null || subscriptionId == null) {
            throw new IllegalArgumentException("When running tests in record mode either 'AZURE_AUTH_LOCATION' or 'AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET and AZURE_SUBSCRIPTION_ID' needs to be set");
        }

        // Create credential using ClientSecretCredential from azure-identity
        TokenCredential credential = new ClientSecretCredentialBuilder()
            .clientId(clientId)
            .tenantId(tenantId)
            .clientSecret(clientSecret)
            .authorityHost(AzureEnvironment.AZURE.getActiveDirectoryEndpoint())
            .build();

        // Create Azure profile for the target subscription and environment
        AzureProfile profile = new AzureProfile(tenantId, subscriptionId, AzureEnvironment.AZURE);

        // Configure and authenticate the AzureResourceManager client
        // The modern SDK handles provider registration and resource tagging internally
        // No need for custom interceptors like ProviderRegistrationInterceptor or ResourceGroupTaggingInterceptor
        AzureResourceManager azureResourceManager = AzureResourceManager
            .configure()
            .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS))
            .authenticate(credential, profile)
            .withSubscription(subscriptionId);

        // The azureResourceManager instance is now ready to use
        // Example: azureResourceManager.resourceGroups().list();
        System.out.println("Azure Resource Manager client initialized successfully");
    }
}
