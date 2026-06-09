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
import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * An HTTP pipeline policy for tagging resource groups created in tests.
 * Migrated from ResourceGroupTaggingInterceptor (OKHttp Interceptor) to HttpPipelinePolicy
 * as required by the modern Azure SDK for Java (com.azure.*).
 */
public class ResourceGroupTaggingPolicy implements HttpPipelinePolicy {
    private static final String LOGGING_CONTEXT = "com.microsoft.azure.management.resources.ResourceGroups createOrUpdate";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
        if ("PUT".equalsIgnoreCase(context.getHttpRequest().getHttpMethod().toString())
                && context.getHttpRequest().getUrl().toString().contains("/resourcegroups/")
                && LOGGING_CONTEXT.equals(context.getHttpRequest().getHeaders().getValue("x-ms-logging-context"))) {

            BinaryData bodyData = context.getHttpRequest().getBodyAsBinaryData();
            if (bodyData == null) {
                return Mono.error(new RuntimeException("Failed to deserialize null body"));
            }
            try {
                String bodyStr = bodyData.toString();
                ObjectNode rgNode = (ObjectNode) objectMapper.readTree(bodyStr);
                if (rgNode == null) {
                    return Mono.error(new RuntimeException("Failed to deserialize " + bodyStr));
                }
                ObjectNode tagsNode;
                if (rgNode.has("tags") && !rgNode.get("tags").isNull()) {
                    tagsNode = (ObjectNode) rgNode.get("tags");
                } else {
                    tagsNode = objectMapper.createObjectNode();
                    rgNode.set("tags", tagsNode);
                }
                tagsNode.put("product", "javasdk");
                tagsNode.put("cause", "automation");
                tagsNode.put("date", OffsetDateTime.now(ZoneOffset.UTC).toString());
                if (System.getenv("ENV_JOB_NAME") != null) {
                    tagsNode.put("job", System.getenv("ENV_JOB_NAME"));
                }
                String newBody = objectMapper.writeValueAsString(rgNode);
                context.getHttpRequest().setBody(BinaryData.fromString(newBody));
                context.getHttpRequest().getHeaders().set("Content-Length",
                        String.valueOf(newBody.getBytes(StandardCharsets.UTF_8).length));
            } catch (Exception e) {
                return Mono.error(new RuntimeException("Failed to process resource group tags", e));
            }
        }
        return next.process();
    }
}
