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
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * An interceptor for tagging resource groups created in tests.
 */
public class ResourceGroupTaggingInterceptor implements HttpPipelinePolicy {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
        HttpRequest request = context.getHttpRequest();
        if ("PUT".equalsIgnoreCase(request.getHttpMethod().toString())
                && request.getUrl().toString().contains("/resourcegroups/")) {
            return FluxUtil.collectBytesInByteBufferStream(request.getBody())
                .flatMap(bytes -> {
                    try {
                        String body = new String(bytes, StandardCharsets.UTF_8);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> rg = objectMapper.readValue(body, Map.class);
                        if (rg == null) {
                            return Mono.error(new RuntimeException("Failed to deserialize " + body));
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, String> tags = (Map<String, String>) rg.get("tags");
                        if (tags == null) {
                            tags = new HashMap<>();
                        }
                        tags.put("product", "javasdk");
                        tags.put("cause", "automation");
                        tags.put("date", OffsetDateTime.now(ZoneOffset.UTC).toString());
                        if (System.getenv("ENV_JOB_NAME") != null) {
                            tags.put("job", System.getenv("ENV_JOB_NAME"));
                        }
                        rg.put("tags", tags);
                        String newBody = objectMapper.writeValueAsString(rg);
                        request.setBody(newBody);
                        request.getHeaders().set("Content-Length", String.valueOf(newBody.getBytes(StandardCharsets.UTF_8).length));
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                    return next.process();
                });
        }
        return next.process();
    }
}
