/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.batch.samples;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.resourcemanager.batch.models.AccountKeyType;
import com.azure.resourcemanager.batch.models.AutoStorageBaseProperties;
import com.azure.resourcemanager.batch.models.BatchAccount;
import com.azure.resourcemanager.batch.models.BatchAccountKeys;
import com.azure.resourcemanager.batch.models.BatchAccountRegenerateKeyParameters;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccountKey;
import com.microsoft.azure.management.samples.Utils;

import java.util.List;

/**
 * Azure Batch sample for managing batch accounts -
 *  - Create a resource group
 *  - List all the batch accounts in subscription.
 *  - Create a batch account with storage account.
 *  - Get the keys for batch account.
 *  - Regenerate keys for batch account
 *  - Regenerate the keys of storage accounts, sync with batch account.
 *  - Create another batch account using existing storage account.
 *  - List the batch account.
 *  - Delete the batch accounts.
 * 
 * Note: Application management is not supported in the new SDK fluent API and has been removed from this sample.
 */

public final class ManageBatchAccount {

    /**
     * Main function which runs the actual sample.
     * @param azureResourceManager instance of the azure client
     * @param batchManager instance of the batch manager
     * @return true if sample runs successfully
     */
    public static boolean runSample(AzureResourceManager azureResourceManager, BatchManager batchManager) {
        final String batchAccountName = "samplebatchacct" + System.currentTimeMillis();
        final String storageAccountName = "samplestore" + System.currentTimeMillis();
        final String batchAccountName2 = "samplebatch2" + System.currentTimeMillis();
        final String rgName = "sample-batch-rg";
        final Region region = Region.AUSTRALIA_SOUTHEAST;
        final Region region2 = Region.US_WEST;

        try {

            // ============================================================
            // Create a resource group

            System.out.println("Creating a resource group");
            
            azureResourceManager.resourceGroups().define(rgName)
                    .withRegion(region)
                    .create();

            System.out.println("Created resource group: " + rgName);

            // ===========================================================
            // List all the batch accounts in subscription.

            System.out.println("Listing existing batch accounts");
            List<BatchAccount> batchAccounts = batchManager.batchAccounts().list().stream().toList();
            System.out.println("Found " + batchAccounts.size() + " batch accounts in subscription");

            // ============================================================
            // Create a storage account for batch

            System.out.println("Creating a storage account for batch");
            
            StorageAccount storageAccount = azureResourceManager.storageAccounts().define(storageAccountName)
                    .withRegion(region)
                    .withExistingResourceGroup(rgName)
                    .create();

            System.out.println("Created storage account: " + storageAccount.name());

            // ============================================================
            // Create a batch account

            System.out.println("Creating a batch Account");

            AutoStorageBaseProperties autoStorageProps = new AutoStorageBaseProperties()
                    .withStorageAccountId(storageAccount.id());

            BatchAccount batchAccount = batchManager.batchAccounts().define(batchAccountName)
                    .withRegion(region)
                    .withExistingResourceGroup(rgName)
                    .withAutoStorage(autoStorageProps)
                    .create();

            System.out.println("Created a batch Account:");
            Utils.print(batchAccount);

            // ============================================================
            // Get | regenerate batch account access keys

            System.out.println("Getting batch account access keys");

            BatchAccountKeys batchAccountKeys = batchManager.batchAccounts().getKeys(rgName, batchAccountName);

            Utils.print(batchAccountKeys);

            System.out.println("Regenerating primary batch account access key");

            BatchAccountRegenerateKeyParameters keyParams = new BatchAccountRegenerateKeyParameters()
                    .withKeyName(AccountKeyType.PRIMARY);
            batchAccountKeys = batchManager.batchAccounts().regenerateKey(rgName, batchAccountName, keyParams);

            Utils.print(batchAccountKeys);

            // ============================================================
            // Regenerate the keys for storage account
            List<StorageAccountKey> storageAccountKeys = storageAccount.getKeys();

            Utils.print(storageAccountKeys);

            System.out.println("Regenerating first storage account access key");

            storageAccountKeys = storageAccount.regenerateKey(storageAccountKeys.get(0).keyName());

            Utils.print(storageAccountKeys);

            // ============================================================
            // Synchronize storage account keys with batch account

            batchManager.batchAccounts().synchronizeAutoStorageKeys(rgName, batchAccountName);

            // ============================================================
            // Create another batch account

            System.out.println("Creating another Batch Account");

            AutoStorageBaseProperties autoStorageProps2 = new AutoStorageBaseProperties()
                    .withStorageAccountId(storageAccount.id());

            BatchAccount batchAccount2 = batchManager.batchAccounts().define(batchAccountName2)
                        .withRegion(region2)
                        .withExistingResourceGroup(rgName)
                        .withAutoStorage(autoStorageProps2)
                        .create();

            System.out.println("Created second Batch Account:");
            Utils.print(batchAccount2);

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
            // Delete a batch account

            System.out.println("Deleting batch accounts");

            batchManager.batchAccounts().deleteById(batchAccount.id());
            System.out.println("Deleted batch account: " + batchAccount.name());

            batchManager.batchAccounts().deleteById(batchAccount2.id());
            System.out.println("Deleted batch account: " + batchAccount2.name());

            return true;
        } catch (Exception f) {
            System.out.println(f.getMessage());
            f.printStackTrace();
        } finally {
            try {
                System.out.println("Deleting Resource Group: " + rgName);
                azureResourceManager.resourceGroups().deleteByName(rgName);
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

            AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

            AzureResourceManager azureResourceManager = AzureResourceManager.configure()
                    .withLogLevel(HttpLogDetailLevel.BASIC)
                    .authenticate(new DefaultAzureCredentialBuilder().build(), profile)
                    .withDefaultSubscription();

            BatchManager batchManager = BatchManager.authenticate(new DefaultAzureCredentialBuilder().build(), profile);

            // Print selected subscription
            System.out.println("Selected subscription: " + azureResourceManager.subscriptionId());

            runSample(azureResourceManager, batchManager);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private ManageBatchAccount() {
    }
}