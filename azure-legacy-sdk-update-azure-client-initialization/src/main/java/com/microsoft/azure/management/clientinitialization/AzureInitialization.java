/**
 * https://github.com/Azure/azure-libraries-for-java/blob/b324fabb9ba2c9687614c800c6ae69e189ce990e/azure-mgmt-resources/src/test/java/com/microsoft/azure/management/resources/core/TestBase.java
 */
package com.microsoft.azure.management.clientinitialization;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.management.resources.core.ResourceGroupTaggingPolicy;

import java.io.File;
import java.io.IOException;

public class AzureInitialization {
    public static void main(String[] args) throws IOException {
        // initialize using env variables
        String clientId = System.getenv("AZURE_CLIENT_ID");
        String tenantId = System.getenv("AZURE_TENANT_ID");
        String clientSecret = System.getenv("AZURE_CLIENT_SECRET");
        String subscriptionId = System.getenv("AZURE_SUBSCRIPTION_ID");
        if (clientId == null || tenantId == null || clientSecret == null || subscriptionId == null) {
            throw new IllegalArgumentException("When running tests in record mode either 'AZURE_AUTH_LOCATION' or 'AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_CLIENT_SECRET and AZURE_SUBSCRIPTION_ID' needs to be set");
        }

        // Modern SDK uses AzureProfile for environment and subscription context
        AzureProfile profile = new AzureProfile(tenantId, subscriptionId, AzureEnvironment.AZURE);
        
        // Modern SDK uses ClientSecretCredential for service principal authentication
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .tenantId(tenantId)
            .build();

        // Configure AzureResourceManager with custom policy and logging
        // Note: Azure (premium client) doesn't need ProviderRegistrationPolicy per migration guide
        AzureResourceManager azure = AzureResourceManager.configure()
            .withPolicy(new ResourceGroupTaggingPolicy())
            .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BODY_AND_HEADERS))
            .authenticate(credential, profile)
            .withSubscription(subscriptionId);

        // initialize using credential file - read JSON with Jackson and construct credential
        final File credentialFile = new File(System.getenv("AZURE_AUTH_LOCATION"));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode credentialFileNode = mapper.readTree(credentialFile);
        String fileClientId = credentialFileNode.get("clientId").asText();
        String fileClientSecret = credentialFileNode.get("clientSecret").asText();
        String fileTenantId = credentialFileNode.get("tenantId").asText();
        String fileSubscriptionId = credentialFileNode.get("subscriptionId").asText();

        AzureProfile fileProfile = new AzureProfile(fileTenantId, fileSubscriptionId, AzureEnvironment.AZURE);
        ClientSecretCredential fileCredential = new ClientSecretCredentialBuilder()
            .clientId(fileClientId)
            .clientSecret(fileClientSecret)
            .tenantId(fileTenantId)
            .build();

        azure = AzureResourceManager.configure()
            .authenticate(fileCredential, fileProfile)
            .withDefaultSubscription();
    }
}
