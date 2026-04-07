/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.resources.core;

import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.util.FluxUtil;
import com.azure.resourcemanager.resources.fluent.models.ResourceGroupInner;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * A pipeline policy for tagging resource groups created in tests.
 * Migrated from legacy OkHttp Interceptor to modern HttpPipelinePolicy.
 */
public class ResourceGroupTaggingPolicy implements HttpPipelinePolicy {
    private static final String LOGGING_CONTEXT = "com.microsoft.azure.management.resources.ResourceGroups createOrUpdate";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
        // Check if this is a resource group PUT request
        if ("PUT".equals(context.getHttpRequest().getHttpMethod().toString()) 
                && context.getHttpRequest().getUrl().toString().contains("/resourcegroups/")
                && LOGGING_CONTEXT.equals(context.getHttpRequest().getHeaders().getValue("x-ms-logging-context"))) {
            
            // Read the request body
            return FluxUtil.collectBytesInByteBufferStream(context.getHttpRequest().getBody())
                .flatMap(bodyBytes -> {
                    try {
                        String body = new String(bodyBytes, StandardCharsets.UTF_8);
                        ResourceGroupInner rg = objectMapper.readValue(body, ResourceGroupInner.class);
                        
                        if (rg == null) {
                            throw new RuntimeException("Failed to deserialize " + body);
                        }
                        
                        // Add tags to the resource group
                        Map<String, String> tags = rg.tags();
                        if (tags == null) {
                            tags = new HashMap<>();
                        }
                        tags.put("product", "javasdk");
                        tags.put("cause", "automation");
                        tags.put("date", OffsetDateTime.now(ZoneOffset.UTC).toString());
                        if (System.getenv("ENV_JOB_NAME") != null) {
                            tags.put("job", System.getenv("ENV_JOB_NAME"));
                        }
                        rg.withTags(tags);

                        // Serialize the modified resource group back to JSON
                        String newBody = objectMapper.writeValueAsString(rg);
                        byte[] newBodyBytes = newBody.getBytes(StandardCharsets.UTF_8);
                        
                        // Update the request body and content-length header
                        context.getHttpRequest().setBody(newBodyBytes);
                        context.getHttpRequest().getHeaders().set("Content-Length", String.valueOf(newBodyBytes.length));
                        
                        return next.process();
                    } catch (IOException e) {
                        return Mono.error(new RuntimeException("Failed to process resource group tagging", e));
                    }
                });
        }
        
        // Not a resource group request, proceed without modification
        return next.process();
    }
}
