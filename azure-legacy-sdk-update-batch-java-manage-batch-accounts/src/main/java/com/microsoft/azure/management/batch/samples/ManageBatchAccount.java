/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.batch.samples;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.batch.models.AccountKeyType;
import com.azure.resourcemanager.batch.models.Application;
import com.azure.resourcemanager.batch.models.ApplicationPackage;
import com.azure.resourcemanager.batch.models.AutoStorageBaseProperties;
import com.azure.resourcemanager.batch.models.BatchAccount;
import com.azure.resourcemanager.batch.models.BatchAccountKeys;
import com.azure.resourcemanager.batch.models.BatchAccountRegenerateKeyParameters;
import com.azure.resourcemanager.storage.StorageManager;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccountKey;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.microsoft.azure.management.samples.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Azure Batch sample for managing batch accounts -
 *  - Get subscription batch account quota for a particular location.
 *  - List all the batch accounts, look if quota allows you to create a new batch account at specified location by counting batch accounts in that particular location.
 *  - Create a batch account with new application and application package, along with new storage account.
 *  - Get the keys for batch account.
 *  - Regenerate keys for batch account
 *  - Regenerate the keys of storage accounts, sync with batch account.
 *  - Update application's display name.
 *  - Create another batch account using existing storage account.
 *  - List the batch account.
 *  - Delete the batch account.
 *      - Delete the application packages.
 *      - Delete applications.
 */

public final class ManageBatchAccount {

    /**
     * Main function which runs the actual sample.
     * @param azure instance of the azure client
     * @param batchManager instance of the batch manager
     * @param storageManager instance of the storage manager
     * @return true if sample runs successfully
     */
    public static boolean runSample(AzureResourceManager azure, BatchManager batchManager, StorageManager storageManager) {
        final String batchAccountName = "samplebatchaccount";
        final String storageAccountName = "samplestorageacct";
        final String applicationName = "application";
        final String applicationDisplayName = "My application display name";
        final String applicationPackageName = "app_package";
        final String batchAccountName2 = "samplebatchaccount2";
        final String rgName = "sample-batch-rg";
        final Region region = Region.AUSTRALIA_SOUTHEAST;
        final Region region2 = Region.US_WEST;

        try {

            // ===========================================================
            // Get how many batch accounts can be created in specified region.
            // Note: getBatchAccountQuotaByLocation is not available in modern SDK
            // Proceeding with batch account creation

            // ===========================================================
            // List all the batch accounts in subscription.

            List<BatchAccount> batchAccounts = batchManager.batchAccounts().list().stream().toList();
            int batchAccountsAtSpecificRegion = 0;
            for (BatchAccount batchAccount: batchAccounts) {
                if (batchAccount.region() == region) {
                    batchAccountsAtSpecificRegion++;
                }
            }

            System.out.println("Found " + batchAccountsAtSpecificRegion + " batch accounts in " + region + " region");

            // ============================================================
            // Create a batch account

            System.out.println("Creating a batch Account");

            // Create resource group
            ResourceGroup resourceGroup = azure.resourceGroups().define(rgName)
                    .withRegion(region)
                    .create();

            // Create storage account
            StorageAccount storageAccount = storageManager.storageAccounts().define(storageAccountName)
                    .withRegion(region)
                    .withExistingResourceGroup(resourceGroup)
                    .create();

            // Create batch account with auto storage
            BatchAccount batchAccount = batchManager.batchAccounts()
                    .define(batchAccountName)
                    .withRegion(region)
                    .withExistingResourceGroup(resourceGroup.name())
                    .withAutoStorage(new AutoStorageBaseProperties().withStorageAccountId(storageAccount.id()))
                    .create();

            // Create application
            Application application = batchManager.applications()
                    .define(applicationName)
                    .withExistingBatchAccount(resourceGroup.name(), batchAccount.name())
                    .withDisplayName(applicationDisplayName)
                    .withAllowUpdates(true)
                    .create();

            // Create application package
            ApplicationPackage applicationPackage = batchManager.applicationPackages()
                    .define(applicationPackageName)
                    .withExistingApplication(resourceGroup.name(), batchAccountName, applicationName)
                    .create();

            System.out.println("Created a batch Account:");
            Utils.print(batchAccount, batchManager);

            // ============================================================
            // Get | regenerate batch account access keys

            System.out.println("Getting batch account access keys");

            BatchAccountKeys batchAccountKeys = batchAccount.getKeys();

            Utils.print(batchAccountKeys);

            System.out.println("Regenerating primary batch account primary access key");

            batchAccountKeys = batchManager.batchAccounts().regenerateKey(rgName, batchAccountName, 
                new BatchAccountRegenerateKeyParameters().withKeyName(AccountKeyType.PRIMARY));

            Utils.print(batchAccountKeys);

            // ============================================================
            // Regenerate the keys for storage account
            storageAccount = storageManager.storageAccounts().getByResourceGroup(rgName, storageAccountName);
            List<StorageAccountKey> storageAccountKeys = storageAccount.getKeys();

            Utils.print(storageAccountKeys);

            System.out.println("Regenerating first storage account access key");

            storageAccountKeys = storageAccount.regenerateKey(storageAccountKeys.get(0).keyName());

            Utils.print(storageAccountKeys);

            // ============================================================
            // Synchronize storage account keys with batch account

            batchAccount.synchronizeAutoStorageKeys();

            // ============================================================
            // Update name of application.
            application = batchManager.applications()
                    .get(rgName, batchAccountName, applicationName)
                    .update()
                    .withDisplayName("New application display name")
                    .apply();

            batchAccount.refresh();
            Utils.print(batchAccount, batchManager);

            // ============================================================
            // Create another batch account

            System.out.println("Creating another Batch Account");

            // Note: getBatchAccountQuotaByLocation is not available in modern SDK
            // Proceeding with batch account creation

            // ===========================================================
            // List all the batch accounts in subscription.

            batchAccounts = batchManager.batchAccounts().list().stream().toList();
            batchAccountsAtSpecificRegion = 0;
            for (BatchAccount batch: batchAccounts) {
                if (batch.region() == region2) {
                    batchAccountsAtSpecificRegion++;
                }
            }

            System.out.println("Found " + batchAccountsAtSpecificRegion + " batch accounts in " + region2 + " region");

            BatchAccount batchAccount2 = batchManager.batchAccounts().define(batchAccountName2)
                    .withRegion(region2)
                    .withExistingResourceGroup(rgName)
                    .withAutoStorage(new AutoStorageBaseProperties().withStorageAccountId(storageAccount.id()))
                    .create();

            System.out.println("Created second Batch Account:");
            Utils.print(batchAccount2, batchManager);

            // ============================================================
            // List batch accounts

            System.out.println("Listing Batch accounts");

            List<BatchAccount> accounts = batchManager.batchAccounts().listByResourceGroup(rgName).stream().toList();
            BatchAccount ba;
            for (int i = 0; i < accounts.size(); i++) {
                ba = accounts.get(i);
                System.out.println("Batch Account (" + i + ") " + ba.name());
            }

            // ============================================================
            // Refresh a batch account.
            batchAccount.refresh();
            Utils.print(batchAccount, batchManager);

            // ============================================================
            // Delete a batch account

            System.out.println("Deleting a batch account - " + batchAccount.name());

            // Delete application package
            System.out.println("Deleting application package - " + applicationPackageName);
            batchManager.applicationPackages().delete(rgName, batchAccountName, applicationName, applicationPackageName);

            // Delete application
            System.out.println("Deleting application - " + applicationName);
            batchManager.applications().delete(rgName, batchAccountName, applicationName);

            batchManager.batchAccounts().deleteById(batchAccount.id());

            System.out.println("Deleted batch account");

            System.out.println("Deleting second batch account - " + batchAccount2.name());
            batchManager.batchAccounts().deleteById(batchAccount2.id());
            System.out.println("Deleted second batch account");

            return true;
        } catch (Exception f) {
            System.out.println(f.getMessage());
            f.printStackTrace();
        } finally {
            try {
                System.out.println("Deleting Resource Group: " + rgName);
                azure.resourceGroups().deleteByName(rgName);
                System.out.println("Deleted Resource Group: " + rgName);
            }
            catch (Exception e) {
                System.out.println("Did not create any resources in Azure. No clean up is necessary");
            }
        }
        return false;
    }

    /**
     * Main entry point.
     * @param args the parameters
     */
    public static void main(String[] args) {

        try {

            final File credFile = new File(System.getenv("AZURE_AUTH_LOCATION"));

            // Parse credential file using Jackson
            ObjectMapper mapper = new ObjectMapper();
            JsonNode credentialFileNode = mapper.readTree(credFile);
            String clientId = credentialFileNode.get("clientId").asText();
            String clientSecret = credentialFileNode.get("clientSecret").asText();
            String tenantId = credentialFileNode.get("tenantId").asText();
            String subscriptionId = credentialFileNode.get("subscriptionId").asText();

            // Create Azure profile and credential
            AzureProfile profile = new AzureProfile(tenantId, subscriptionId, AzureEnvironment.AZURE);
            ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

            // Initialize managers
            AzureResourceManager azure = AzureResourceManager.configure()
                .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BASIC))
                .authenticate(credential, profile)
                .withSubscription(subscriptionId);

            BatchManager batchManager = BatchManager.configure()
                .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BASIC))
                .authenticate(credential, profile);

            StorageManager storageManager = StorageManager.configure()
                .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BASIC))
                .authenticate(credential, profile);

            // Print selected subscription
            System.out.println("Selected subscription: " + azure.subscriptionId());

            runSample(azure, batchManager, storageManager);
        } catch (IOException e) {
            System.out.println("Failed to read credential file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private ManageBatchAccount() {
    }
}