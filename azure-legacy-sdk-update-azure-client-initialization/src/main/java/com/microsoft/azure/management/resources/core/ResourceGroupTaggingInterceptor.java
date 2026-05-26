/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.management.resources.core;

import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.util.FluxUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * An HTTP pipeline policy for tagging resource groups created in tests.
 * Migrated from OkHttp Interceptor to com.azure.core HttpPipelinePolicy.
 */
public class ResourceGroupTaggingInterceptor implements HttpPipelinePolicy {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
        HttpRequest request = context.getHttpRequest();
        if ("PUT".equals(request.getHttpMethod().name())
                && request.getUrl().toString().contains("/resourcegroups/")) {
            return FluxUtil.collectBytesInByteBufferStream(request.getBody())
                .flatMap(bytes -> {
                    String body = new String(bytes, StandardCharsets.UTF_8);
                    try {
                        ObjectNode rg = (ObjectNode) objectMapper.readTree(body);
                        if (rg == null) {
                            throw new RuntimeException("Failed to deserialize " + body);
                        }
                        ObjectNode tags = (rg.has("tags") && !rg.get("tags").isNull())
                            ? (ObjectNode) rg.get("tags")
                            : objectMapper.createObjectNode();
                        tags.put("product", "javasdk");
                        tags.put("cause", "automation");
                        tags.put("date", ZonedDateTime.now(ZoneOffset.UTC).toString());
                        String jobName = System.getenv("ENV_JOB_NAME");
                        if (jobName != null) {
                            tags.put("job", jobName);
                        }
                        rg.set("tags", tags);
                        String newBody = objectMapper.writeValueAsString(rg);
                        request.setBody(newBody);
                        return next.process();
                    } catch (Exception e) {
                        return Mono.<HttpResponse>error(new RuntimeException("Failed to process request body", e));
                    }
                });
        }
        return next.process();
    }
}
