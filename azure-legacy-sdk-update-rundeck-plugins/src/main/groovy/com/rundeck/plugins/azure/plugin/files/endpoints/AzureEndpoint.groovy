package com.rundeck.plugins.azure.plugin.files.endpoints

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.rundeck.plugins.azure.plugin.files.EndpointHandler
import com.rundeck.plugins.azure.plugin.files.URIParser

/**
 * Created by luistoledo on 11/14/17.
 */
class AzureEndpoint {
    public static EndpointHandler createEndpointHandler(final URIParser url, String storageName, String accessKey) throws IOException {

        String storageConnectionString = "DefaultEndpointsProtocol=http;AccountName=" + storageName+ ";AccountKey=" + accessKey;

        String containerName = url.getHost()

        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildClient()

        BlobContainerClient container = serviceClient.getBlobContainerClient(containerName)
        if (!container.exists()) {
            container.create()
        }

        OutputStream outputStream=null
        File tempFile=null
        String destinationPath=null

        boolean doUploading=false


        return new EndpointHandler() {
            @Override
            List<String> listFiles(String path) throws IOException {
                List<String> list = new ArrayList<>()
                container.listBlobs().each {blobItem->
                    list.add("/" + blobItem.getName())
                }
                return list
            }

            @Override
            InputStream newTransferInputStream(String path) throws IOException {
                return download(path)
            }

            @Override
            OutputStream newTransferOutputStream(String path) throws IOException {
                destinationPath=path
                tempFile = File.createTempFile("azure-transfer", "tmp", null);
                outputStream = new BufferedOutputStream(new FileOutputStream(tempFile.getAbsolutePath()));

                doUploading=true

                return outputStream
            }

            @Override
            boolean finishTransferTransaction() throws IOException {
                if(doUploading){
                    upload()
                }
                return false
            }

            @Override
            boolean deleteFile(String path) throws IOException {
                return false
            }

            @Override
            void disconnect() throws IOException {

            }

            @Override
            boolean fileExists(String path) throws IOException {
                String fileName = path.substring(1,path.length())
                BlobClient blob = container.getBlobClient(fileName)
                return blob.exists()
            }

            boolean upload() throws IOException {

                String fileName = destinationPath.substring(1,destinationPath.length())

                tempFile=new File(tempFile.getAbsolutePath())

                BlobClient blob = container.getBlobClient(fileName)
                blob.upload(new FileInputStream(tempFile), tempFile.length(), true)

                tempFile.delete()

                return true
            }

            InputStream download(String path) throws IOException {
                String fileName = path.substring(1,path.length())

                BlobClient blob = container.getBlobClient(fileName)

                tempFile = File.createTempFile("azure-transfer", "tmp", null);
                blob.downloadStream(new FileOutputStream(tempFile))

                InputStream result = new BufferedInputStream(new FileInputStream(tempFile.getAbsolutePath()))

                tempFile.delete()

                return result;
            }
        }

    }
}
