/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.resources.core;

import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * A pipeline policy for tagging resource groups created in tests.
 * Migrated from ResourceGroupTaggingInterceptor (OKHttp Interceptor) to
 * HttpPipelinePolicy (azure-core).
 */
public class ResourceGroupTaggingPolicy implements HttpPipelinePolicy {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
        HttpRequest request = context.getHttpRequest();
        // The legacy SDK set "x-ms-logging-context" automatically; the modern SDK does not.
        // Use the PUT + /resourcegroups/ URL check alone to identify resource group create/update operations.
        if ("PUT".equals(request.getHttpMethod().toString())
                && request.getUrl().toString().contains("/resourcegroups/")) {
            try {
                BinaryData body = request.getBodyAsBinaryData();
                String bodyStr = body != null ? body.toString() : null;
                if (bodyStr == null) {
                    throw new RuntimeException("Failed to read request body");
                }
                JsonNode jsonNode = mapper.readTree(bodyStr);
                if (!(jsonNode instanceof ObjectNode)) {
                    throw new RuntimeException("Failed to deserialize " + bodyStr);
                }
                ObjectNode obj = (ObjectNode) jsonNode;
                JsonNode tagsNode = obj.get("tags");
                ObjectNode tags;
                if (tagsNode == null || tagsNode.isNull() || !tagsNode.isObject()) {
                    tags = mapper.createObjectNode();
                    obj.set("tags", tags);
                } else {
                    tags = (ObjectNode) tagsNode;
                }
                tags.put("product", "javasdk");
                tags.put("cause", "automation");
                tags.put("date", OffsetDateTime.now(ZoneOffset.UTC).toString());
                if (System.getenv("ENV_JOB_NAME") != null) {
                    tags.put("job", System.getenv("ENV_JOB_NAME"));
                }
                String newBody = mapper.writeValueAsString(jsonNode);
                request.setBody(BinaryData.fromString(newBody));
            } catch (Exception e) {
                return Mono.error(e);
            }
        }
        return next.process();
    }
}
