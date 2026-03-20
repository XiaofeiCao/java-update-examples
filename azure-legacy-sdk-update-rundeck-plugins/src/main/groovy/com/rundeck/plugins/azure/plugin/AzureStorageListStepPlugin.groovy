package com.rundeck.plugins.azure.plugin

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobItem
import com.azure.storage.blob.models.BlobListDetails
import com.azure.storage.blob.models.ListBlobsOptions
import com.rundeck.plugins.azure.util.AzurePluginUtil
import groovy.json.JsonOutput

/**
 * Created by luistoledo on 11/15/17.
 */
@Plugin(name = AzureStorageListStepPlugin.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
class AzureStorageListStepPlugin implements StepPlugin, Describable {
    public static final String PROVIDER_NAME = "azure-local-list-step";
    public static final String PROVIDER_TITLE = "Azure / Storage / List"
    public static final String PROVIDER_DESCRIPTION ="List blobs form Azure Storage Container"

    public static final String STORAGE_NAME = "storage"
    public static final String ACCESS_KEY = "key"
    public static final String CONTAINER_NAME = "containerName"
    public static final String RECURSIVE = "recursive"

    final static Map<String, Object> renderingOptionsAuthentication = AzurePluginUtil.getRenderOpt("Credentials",false)
    final static Map<String, Object> renderingOptionsAuthenticationStorage = AzurePluginUtil.getRenderOpt("Credentials",false, false, true)
    final static Map<String, Object> renderingOptionsConfig = AzurePluginUtil.getRenderOpt("Configuration",false)


    static Description DESCRIPTION = DescriptionBuilder.builder()
            .name(PROVIDER_NAME)
            .title(PROVIDER_TITLE)
            .description(PROVIDER_DESCRIPTION)
            .property(PropertyUtil.string(STORAGE_NAME, "Storage Account", "Azure Storage Account", true,
            null,null,null, renderingOptionsAuthentication))
            .property(PropertyUtil.string(ACCESS_KEY, "Access Key", "Azure Storage Access Key", true,
            null,null,null, renderingOptionsAuthenticationStorage))
            .property(PropertyUtil.string(CONTAINER_NAME, "Container Name", "Container Name form the Azure Storage", false,
            null,null,null, renderingOptionsConfig))
            .property(PropertyUtil.bool(RECURSIVE, "Recursive", "Show content of subfolders", false,
            "false",null, renderingOptionsConfig))
            .build()


    @Override
    Description getDescription() {
        return DESCRIPTION
    }

    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {

        String storageName=configuration.get(AzureStorageListStepPlugin.STORAGE_NAME)
        String accessKeyStoragePath=configuration.get(AzureStorageListStepPlugin.ACCESS_KEY)
        String containerName=configuration.get(AzureStorageListStepPlugin.CONTAINER_NAME)
        boolean recursive=Boolean.valueOf(configuration.get(AzureStorageListStepPlugin.RECURSIVE))

        String accessKey = AzurePluginUtil.getPasswordFromKeyStorage(accessKeyStoragePath,context);

        String storageConnectionString = "DefaultEndpointsProtocol=http;AccountName=" + storageName + ";AccountKey=" + accessKey;

        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildClient()
        BlobContainerClient container = null
        try{
            container = serviceClient.getBlobContainerClient(containerName)
        }catch(Exception e){
            throw new IllegalArgumentException("Error getting the container Name");
        }

        List list = new ArrayList()

        if(recursive){
            container.listBlobs().each { BlobItem blobItem ->
                list.add(printBlob(blobItem, containerName))
            }
        }else{
            container.listBlobsByHierarchy("/").each { blobItem ->
                if(blobItem.isPrefix()){
                    list.add([name:blobItem.getName(),
                              container:containerName,
                              uri:"",
                              lastModified:"",
                              length:"",
                              type:"FOLDER",
                              contentType:""])
                }else{
                    list.add(printBlob(blobItem, containerName))
                }
            }
        }

        def json = JsonOutput.toJson(list)

        Map<String, String> meta = new HashMap<>();
        meta.put("content-data-type", "application/json");
        context.getExecutionContext().getExecutionListener().log(2, json, meta);
    }


    def printBlob = { BlobItem blobItem, String containerNameRef ->
        return [name:blobItem.getName(),
                container:containerNameRef,
                uri:"",
                lastModified:blobItem.getProperties()?.getLastModified()?.format("yyyy/MM/dd HH:mm:ss"),
                length:blobItem.getProperties()?.getContentLength(),
                type:blobItem.getProperties()?.getBlobType(),
                contentType:blobItem.getProperties()?.getContentType()]

    }
    def printMetadata ={meta->
        StringBuffer buffer = new StringBuffer()

        meta.each{key,value->
            buffer.append("${key}: ${value}")
        }

        return buffer.toString()
    }

}
