/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.batch.samples;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.batch.BatchManager;
import com.azure.resourcemanager.batch.models.AccountKeyType;
import com.azure.resourcemanager.batch.models.Application;
import com.azure.resourcemanager.batch.models.ApplicationPackage;
import com.azure.resourcemanager.batch.models.AutoStorageBaseProperties;
import com.azure.resourcemanager.batch.models.BatchAccount;
import com.azure.resourcemanager.batch.models.BatchAccountKeys;
import com.azure.resourcemanager.batch.models.BatchAccountRegenerateKeyParameters;
import com.azure.resourcemanager.batch.models.BatchLocationQuota;
import com.microsoft.azure.management.samples.Utils;
import com.azure.resourcemanager.storage.models.StorageAccount;
import com.azure.resourcemanager.storage.models.StorageAccountKey;

import java.util.ArrayList;
import java.util.List;

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
     * @param azureResourceManager instance of the azure resource manager client
     * @param batchManager instance of the batch manager client
     * @return true if sample runs successfully
     */
    public static boolean runSample(AzureResourceManager azureResourceManager, BatchManager batchManager) {
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

            BatchLocationQuota locationQuota = batchManager.locations().getQuotas(region.name());
            int allowedNumberOfBatchAccounts = locationQuota.accountQuota();

            // ===========================================================
            // List all the batch accounts in subscription.

            List<BatchAccount> batchAccounts = new ArrayList<>();
            batchManager.batchAccounts().list().forEach(batchAccounts::add);
            int batchAccountsAtSpecificRegion = 0;
            for (BatchAccount ba : batchAccounts) {
                if (region.name().equals(ba.location())) {
                    batchAccountsAtSpecificRegion++;
                }
            }

            if (batchAccountsAtSpecificRegion >= allowedNumberOfBatchAccounts) {
                System.out.println("No more batch accounts can be created at "
                        + region + " region, this region already have "
                        + batchAccountsAtSpecificRegion
                        + " batch accounts, current quota to create batch account in "
                        + region + " region is " + allowedNumberOfBatchAccounts + ".");
                return false;
            }

            // ============================================================
            // Create a resource group

            System.out.println("Creating a resource group");

            azureResourceManager.resourceGroups().define(rgName)
                    .withRegion(region)
                    .create();

            // ============================================================
            // Create a storage account for the batch account

            System.out.println("Creating a Storage Account");

            StorageAccount storageAccount = azureResourceManager.storageAccounts().define(storageAccountName)
                    .withRegion(region)
                    .withExistingResourceGroup(rgName)
                    .create();

            // ============================================================
            // Create a batch account

            System.out.println("Creating a batch Account");

            BatchAccount batchAccount = batchManager.batchAccounts().define(batchAccountName)
                    .withRegion(region.name())
                    .withExistingResourceGroup(rgName)
                    .withAutoStorage(new AutoStorageBaseProperties()
                            .withStorageAccountId(storageAccount.id()))
                    .create();

            System.out.println("Created a batch Account:");
            Utils.print(batchAccount);

            // ============================================================
            // Create an application on the batch account

            System.out.println("Creating an application");

            Application application = batchManager.applications().define(applicationName)
                    .withExistingBatchAccount(rgName, batchAccountName)
                    .withAllowUpdates(true)
                    .withDisplayName(applicationDisplayName)
                    .create();

            // ============================================================
            // Create an application package

            System.out.println("Creating an application package");

            batchManager.applicationPackages()
                    .define(applicationPackageName)
                    .withExistingApplication(rgName, batchAccountName, applicationName)
                    .create();

            batchAccount = batchAccount.refresh();
            Utils.print(batchAccount);

            // ============================================================
            // Get | regenerate batch account access keys

            System.out.println("Getting batch account access keys");

            BatchAccountKeys batchAccountKeys = batchAccount.getKeys();

            Utils.print(batchAccountKeys);

            System.out.println("Regenerating primary batch account primary access key");

            batchAccountKeys = batchAccount.regenerateKey(
                    new BatchAccountRegenerateKeyParameters().withKeyName(AccountKeyType.PRIMARY));

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

            batchAccount.synchronizeAutoStorageKeys();

            // ============================================================
            // Update name of application.

            application.update()
                    .withDisplayName("New application display name")
                    .apply();

            batchAccount = batchAccount.refresh();
            Utils.print(batchAccount);

            // ============================================================
            // Create another batch account

            System.out.println("Creating another Batch Account");

            locationQuota = batchManager.locations().getQuotas(region2.name());
            allowedNumberOfBatchAccounts = locationQuota.accountQuota();

            // ===========================================================
            // List all the batch accounts in subscription.

            batchAccounts.clear();
            batchManager.batchAccounts().list().forEach(batchAccounts::add);
            batchAccountsAtSpecificRegion = 0;
            for (BatchAccount batch : batchAccounts) {
                if (region2.name().equals(batch.location())) {
                    batchAccountsAtSpecificRegion++;
                }
            }

            BatchAccount batchAccount2 = null;
            if (batchAccountsAtSpecificRegion < allowedNumberOfBatchAccounts) {
                batchAccount2 = batchManager.batchAccounts().define(batchAccountName2)
                        .withRegion(region2.name())
                        .withExistingResourceGroup(rgName)
                        .withAutoStorage(new AutoStorageBaseProperties()
                                .withStorageAccountId(storageAccount.id()))
                        .create();

                System.out.println("Created second Batch Account:");
                Utils.print(batchAccount2);
            }

            // ============================================================
            // List batch accounts

            System.out.println("Listing Batch accounts");

            List<BatchAccount> accounts = new ArrayList<>();
            batchManager.batchAccounts().listByResourceGroup(rgName).forEach(accounts::add);
            for (int i = 0; i < accounts.size(); i++) {
                System.out.println("Batch Account (" + i + ") " + accounts.get(i).name());
            }

            // ============================================================
            // Refresh a batch account.
            batchAccount = batchAccount.refresh();
            Utils.print(batchAccount);

            // ============================================================
            // Delete a batch account

            System.out.println("Deleting a batch account - " + batchAccount.name());

            for (ApplicationPackage pkg : batchManager.applicationPackages()
                    .list(rgName, batchAccountName, applicationName)) {
                System.out.println("Deleting an application package - " + pkg.name());
                batchManager.applicationPackages().delete(rgName, batchAccountName, applicationName, pkg.name());
            }

            for (Application app : batchManager.applications().list(rgName, batchAccountName)) {
                System.out.println("Deleting an application - " + app.name());
                batchManager.applications().delete(rgName, batchAccountName, app.name());
            }

            batchManager.batchAccounts().deleteByResourceGroup(rgName, batchAccountName);

            System.out.println("Deleted batch account");

            if (batchAccount2 != null) {
                System.out.println("Deleting second batch account - " + batchAccount2.name());
                batchManager.batchAccounts().deleteByResourceGroup(rgName, batchAccountName2);
                System.out.println("Deleted second batch account");
            }

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
            TokenCredential credential = new DefaultAzureCredentialBuilder().build();

            AzureResourceManager azureResourceManager = AzureResourceManager.configure()
                    .withLogLevel(HttpLogDetailLevel.BASIC)
                    .authenticate(credential, profile)
                    .withDefaultSubscription();

            BatchManager batchManager = BatchManager.configure()
                    .withLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BASIC))
                    .authenticate(credential, profile);

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