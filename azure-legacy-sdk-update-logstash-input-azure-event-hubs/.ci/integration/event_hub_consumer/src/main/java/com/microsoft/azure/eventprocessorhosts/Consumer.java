package com.microsoft.azure.eventprocessorhosts;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.models.CloseContext;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.messaging.eventhubs.models.InitializationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Uses modern EventProcessorClient with in-memory checkpoint store.
 */
public class Consumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);
    private static final AtomicLong totalCount = new AtomicLong();

    /**
     * Usage:
     * cd integration/event_hub_consumer
     * mvn package
     * java -jar target/event-hub-consumer.jar "[your_connection_string_here]"
     */
    public static void main(String... args) {

        try {
            if (args.length != 1 || !args[0].startsWith("Endpoint=sb") || !args[0].contains("EntityPath")) {
                LOGGER.error("The first and only argument must be the event hub connection string with the EntityPath. For example:");
                LOGGER.error("Endpoint=sb://logstash-demo.servicebus.windows.net/;SharedAccessKeyName=activity-log-ro;SharedAccessKey=<redacted>;EntityPath=my_event_hub");
                System.exit(1);
            }

            LOGGER.debug("Consuming events from Event Hub ...");

            // Create in-memory checkpoint store
            InMemoryCheckpointStore checkpointStore = new InMemoryCheckpointStore();

            EventProcessorClient processor = new EventProcessorClientBuilder()
                    .connectionString(args[0])
                    .consumerGroup("$Default")
                    .checkpointStore(checkpointStore)
                    .processEvent(eventContext -> onEvent(eventContext))
                    .processError(context -> onError(context))
                    .processPartitionInitialization(initializationContext -> onOpen(initializationContext))
                    .processPartitionClose(closeContext -> onClose(closeContext))
                    .buildEventProcessorClient();

            LOGGER.debug("Starting processor...");
            processor.start();

            System.out.println("Press enter to stop !");
            System.in.read();

            LOGGER.debug("Stopping processor...");
            processor.stop();

            LOGGER.info("Done reading. Thanks for playing. ");
        } catch (Throwable t) {
            LOGGER.error("Something bad just happened :(", t);
        }
    }

    private static void onEvent(EventContext eventContext) {
        LOGGER.debug("Partition {} got event", eventContext.getPartitionContext().getPartitionId());
        try {
            LOGGER.debug("Received event: {}", new String(eventContext.getEventData().getBody(), StandardCharsets.UTF_8));
            totalCount.incrementAndGet();
            LOGGER.info("************* Consumed {} total events (so far) **********", totalCount);
        } catch (Exception e) {
            LOGGER.error("Processing failed for an event ", e);
        }
    }

    private static void onError(ErrorContext context) {
        LOGGER.error("Partition {} error", context.getPartitionContext().getPartitionId(), context.getThrowable());
    }

    private static void onOpen(InitializationContext context) {
        LOGGER.debug("Partition {} is opening", context.getPartitionContext().getPartitionId());
    }

    private static void onClose(CloseContext context) {
        LOGGER.debug("Partition {} is closing for reason {} ", 
            context.getPartitionContext().getPartitionId(), context.getCloseReason());
    }
}
