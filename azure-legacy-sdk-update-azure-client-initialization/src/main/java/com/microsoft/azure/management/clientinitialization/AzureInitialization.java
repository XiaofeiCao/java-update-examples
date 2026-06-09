/**
 * https://github.com/Azure/azure-libraries-for-java/blob/b324fabb9ba2c9687614c800c6ae69e189ce990e/azure-mgmt-resources/src/test/java/com/microsoft/azure/management/resources/core/TestBase.java
 */
package com.microsoft.azure.management.clientinitialization;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.management.resources.core.ResourceGroupTaggingPolicy;

import java.io.IOException;

public class AzureInitialization {
    public static void main(String[] args) throws IOException {
        // initialize using env
        String clientId = System.getenv("AZURE_CLIENT_ID");
        String tenantId = System.getenv("AZURE_TENANT_ID");
        String clientSecret = System.getenv("AZURE_CLIENT_SECRET");
        String subscriptionId = System.getenv("AZURE_SUBSCRIPTION_ID");
        if (clientId == null || tenantId == null || clientSecret == null || subscriptionId == null) {
            throw new IllegalArgumentException("When running tests in record mode 'AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET and AZURE_SUBSCRIPTION_ID' needs to be set");
        }

        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);
        // TODO: The original code authenticated using a credential file (AZURE_AUTH_LOCATION),
        // which is discouraged because it relies on long-lived secrets on disk and conflicts
        // with Azure's security-by-default guidance. It has been replaced with
        // DefaultAzureCredential. This change alters the authentication mechanism, so the
        // resulting code path requires extra testing (local dev, CI, and target runtime
        // identities) before it is considered production-ready.
        TokenCredential credential = new DefaultAzureCredentialBuilder().build();

        // Azure is in the built-in provider registration list, so ProviderRegistrationPolicy is not needed.
        AzureResourceManager azure = AzureResourceManager.configure()
            .withPolicy(new ResourceGroupTaggingPolicy())
            .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS))
            .authenticate(credential, profile)
            .withDefaultSubscription();
    }
}

