/**
 * https://github.com/Azure/azure-libraries-for-java/blob/b324fabb9ba2c9687614c800c6ae69e189ce990e/azure-mgmt-resources/src/test/java/com/microsoft/azure/management/resources/core/TestBase.java
 */
package com.microsoft.azure.management.clientinitialization;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.microsoft.azure.management.resources.core.ResourceGroupTaggingPolicy;

public class AzureInitialization {
    public static void main(String[] args) {
        String subscriptionId = System.getenv("AZURE_SUBSCRIPTION_ID");
        if (subscriptionId == null) {
            throw new IllegalArgumentException(
                "AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET and AZURE_SUBSCRIPTION_ID needs to be set");
        }

        // TODO: The original code authenticated using environment variables (AZURE_CLIENT_ID, AZURE_TENANT_ID,
        // AZURE_CLIENT_SECRET) and a credential file (AZURE_AUTH_LOCATION), which is discouraged because
        // it relies on long-lived secrets on disk and conflicts with Azure's security-by-default guidance.
        // It has been replaced with DefaultAzureCredential. This change alters the authentication mechanism,
        // so the resulting code path requires extra testing (local dev, CI, and target runtime identities)
        // before it is considered production-ready.
        TokenCredential credential = new DefaultAzureCredentialBuilder().build();
        AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

        AzureResourceManager azure = AzureResourceManager.configure()
            .withPolicy(new ResourceGroupTaggingPolicy())
            .authenticate(credential, profile)
            .withSubscription(subscriptionId);
    }
}

