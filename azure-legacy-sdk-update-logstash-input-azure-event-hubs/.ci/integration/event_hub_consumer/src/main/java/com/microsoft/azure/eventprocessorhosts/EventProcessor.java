package com.microsoft.azure.eventprocessorhosts;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.messaging.eventhubs.models.PartitionContext;
import com.azure.messaging.eventhubs.models.CloseContext;
import com.azure.messaging.eventhubs.models.InitializationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public class EventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventProcessor.class);
    private static final AtomicLong totalCount = new AtomicLong();

    public void onOpen(InitializationContext context) {
        LOGGER.debug("Partition {} is opening", context.getPartitionContext().getPartitionId());
    }

    public void onClose(CloseContext context) {
        LOGGER.debug("Partition {} is closing for reason {} ", 
                context.getPartitionContext().getPartitionId(), 
                context.getCloseReason().toString());
    }

    public void onEvent(EventContext eventContext) {
        PartitionContext partitionContext = eventContext.getPartitionContext();
        EventData event = eventContext.getEventData();
        
        try {
            LOGGER.debug("Received event: {}", new String(event.getBody(), StandardCharsets.UTF_8));
            totalCount.incrementAndGet();
            // not check pointing here on purpose
        } catch (Exception e) {
            LOGGER.error("Processing failed for an event ", e);
        }
        
        LOGGER.info("************* Consumed {} total events (so far) **********", totalCount);
    }
}
