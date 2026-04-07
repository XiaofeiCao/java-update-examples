package com.microsoft.azure.eventprocessorhosts;

import com.azure.messaging.eventhubs.models.ErrorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorNotificationHandler.class);

    public void onError(ErrorContext errorContext) {
        LOGGER.error("Partition {} received error during {}: {}", 
                errorContext.getPartitionContext().getPartitionId(),
                "processing",
                errorContext.getThrowable() != null ? errorContext.getThrowable().getMessage() : "Unknown error",
                errorContext.getThrowable());
    }
}
