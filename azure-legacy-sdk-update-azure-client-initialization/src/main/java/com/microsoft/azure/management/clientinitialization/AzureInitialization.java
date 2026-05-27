/**
 * https://github.com/Azure/azure-libraries-for-java/blob/b324fabb9ba2c9687614c800c6ae69e189ce990e/azure-mgmt-resources/src/test/java/com/microsoft/azure/management/resources/core/TestBase.java
 */
package com.microsoft.azure.management.clientinitialization;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.management.resources.core.ResourceGroupTaggingInterceptor;

import java.time.Duration;

public class AzureInitialization {
    public static void main(String[] args) {
        // initialize using env
        String clientId = System.getenv("AZURE_CLIENT_ID");
        String tenantId = System.getenv("AZURE_TENANT_ID");
        String clientSecret = System.getenv("AZURE_CLIENT_SECRET");
        String subscriptionId = System.getenv("AZURE_SUBSCRIPTION_ID");
        if (clientId == null || tenantId == null || clientSecret == null || subscriptionId == null) {
            throw new IllegalArgumentException("When running tests in record mode either 'AZURE_AUTH_LOCATION' or 'AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET and AZURE_SUBSCRIPTION_ID' needs to be set");
        }

        TokenCredential credential = new ClientSecretCredentialBuilder()
            .clientId(clientId)
            .tenantId(tenantId)
            .clientSecret(clientSecret)
            .build();

        AzureProfile profile = new AzureProfile(tenantId, subscriptionId, AzureEnvironment.AZURE);
        AzureResourceManager azure = AzureResourceManager
            .configure()
            .withLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS)
            .withPolicy(new ResourceGroupTaggingInterceptor())
            .withReadTimeout(Duration.ofMinutes(3))
            .authenticate(credential, profile)
            .withSubscription(subscriptionId);

        // initialize using credential file
        TokenCredential defaultCredential = new DefaultAzureCredentialBuilder().build();
        AzureProfile defaultProfile = new AzureProfile(AzureEnvironment.AZURE);
        azure = AzureResourceManager
            .authenticate(defaultCredential, defaultProfile)
            .withDefaultSubscription();
    }
}
