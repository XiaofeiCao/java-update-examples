package com.microsoft.azure.eventprocessorhosts;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.inmemory.InMemoryCheckpointStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses EventProcessorClient with in-memory checkpoint store.
 * This only works as a single standalone consumer using the $Default consumer group.
 */
public class Consumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);

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
            
            LOGGER.debug("Consuming events from Event Hub...");
            InMemoryCheckpointStore checkpointStore = new InMemoryCheckpointStore();
            EventProcessor eventProcessor = new EventProcessor();
            ErrorNotificationHandler errorHandler = new ErrorNotificationHandler();

            EventProcessorClient processor = new EventProcessorClientBuilder()
                    .connectionString(args[0])
                    .consumerGroup("$Default")
                    .checkpointStore(checkpointStore)
                    .processEvent(eventContext -> eventProcessor.onEvent(eventContext))
                    .processError(context -> errorHandler.onError(context))
                    .processPartitionInitialization(initializationContext -> eventProcessor.onOpen(initializationContext))
                    .processPartitionClose(closeContext -> eventProcessor.onClose(closeContext))
                    .buildEventProcessorClient();

            LOGGER.debug("Starting event processor...");
            processor.start();

            System.out.println("Press enter to stop !");
            System.in.read();

            LOGGER.debug("Stopping event processor...");
            processor.stop();

            LOGGER.info("Done reading. Thanks for playing. ");
        } catch (Throwable t) {
            LOGGER.error("Something bad just happened :(", t);
        }
    }

}
