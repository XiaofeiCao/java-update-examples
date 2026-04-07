package com.microsoft.azure.eventprocessorhosts;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Uses EventProcessorClient with in-memory checkpoint store.
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

            // Create the EventProcessorClient
            EventProcessorClient eventProcessorClient = new EventProcessorClientBuilder()
                    .connectionString(args[0])
                    .consumerGroup("$Default")
                    .processEvent(context -> processEvent(context))
                    .processError(context -> processError(context))
                    .buildEventProcessorClient();

            LOGGER.debug("Starting event processor...");
            eventProcessorClient.start();

            System.out.println("Press enter to stop !");
            System.in.read();

            LOGGER.debug("Stopping event processor...");
            eventProcessorClient.stop();

            LOGGER.info("Done reading. Thanks for playing. ");
        } catch (Throwable t) {
            LOGGER.error("Something bad just happened :(", t);
        }
    }

    private static void processEvent(EventContext eventContext) {
        try {
            if (eventContext.getEventData() != null) {
                String eventBody = new String(eventContext.getEventData().getBody(), StandardCharsets.UTF_8);
                LOGGER.debug("Partition {}: Received event: {}", 
                        eventContext.getPartitionContext().getPartitionId(), 
                        eventBody);
                
                totalCount.incrementAndGet();
                LOGGER.info("************* Consumed {} total events (so far) **********", totalCount);
                
                // Not checkpointing on purpose for this demo
                // To checkpoint: eventContext.updateCheckpoint();
            }
        } catch (Exception e) {
            LOGGER.error("Error processing event", e);
        }
    }

    private static void processError(ErrorContext errorContext) {
        LOGGER.error("Error on partition {}: {}", 
                errorContext.getPartitionContext().getPartitionId(),
                errorContext.getThrowable().getMessage(),
                errorContext.getThrowable());
    }

}
