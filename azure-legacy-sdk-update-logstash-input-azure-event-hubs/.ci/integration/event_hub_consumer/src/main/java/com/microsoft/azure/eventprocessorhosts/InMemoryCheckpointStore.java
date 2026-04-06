package com.microsoft.azure.eventprocessorhosts;

import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.models.Checkpoint;
import com.azure.messaging.eventhubs.models.PartitionOwnership;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Simple in-memory implementation of CheckpointStore for testing purposes.
 * This is NOT recommended for production use - use BlobCheckpointStore instead.
 */
public class InMemoryCheckpointStore implements CheckpointStore {

    private final Map<String, PartitionOwnership> ownershipMap = new ConcurrentHashMap<>();
    private final Map<String, Checkpoint> checkpointMap = new ConcurrentHashMap<>();

    @Override
    public Flux<PartitionOwnership> listOwnership(String fullyQualifiedNamespace, String eventHubName, String consumerGroup) {
        return Flux.fromIterable(
            ownershipMap.values()
                .stream()
                .filter(po -> po.getFullyQualifiedNamespace().equals(fullyQualifiedNamespace)
                    && po.getEventHubName().equals(eventHubName)
                    && po.getConsumerGroup().equals(consumerGroup))
                .collect(Collectors.toList())
        );
    }

    @Override
    public Flux<PartitionOwnership> claimOwnership(List<PartitionOwnership> requestedPartitionOwnerships) {
        return Flux.fromIterable(requestedPartitionOwnerships)
            .map(po -> {
                String key = getOwnershipKey(po.getFullyQualifiedNamespace(), po.getEventHubName(), 
                    po.getConsumerGroup(), po.getPartitionId());
                ownershipMap.put(key, po);
                return po;
            });
    }

    @Override
    public Flux<Checkpoint> listCheckpoints(String fullyQualifiedNamespace, String eventHubName, String consumerGroup) {
        return Flux.fromIterable(
            checkpointMap.values()
                .stream()
                .filter(cp -> cp.getFullyQualifiedNamespace().equals(fullyQualifiedNamespace)
                    && cp.getEventHubName().equals(eventHubName)
                    && cp.getConsumerGroup().equals(consumerGroup))
                .collect(Collectors.toList())
        );
    }

    @Override
    public Mono<Void> updateCheckpoint(Checkpoint checkpoint) {
        String key = getCheckpointKey(checkpoint.getFullyQualifiedNamespace(), checkpoint.getEventHubName(), 
            checkpoint.getConsumerGroup(), checkpoint.getPartitionId());
        checkpointMap.put(key, checkpoint);
        return Mono.empty();
    }

    private String getOwnershipKey(String fullyQualifiedNamespace, String eventHubName, 
                                   String consumerGroup, String partitionId) {
        return String.format("%s/%s/%s/%s", fullyQualifiedNamespace, eventHubName, consumerGroup, partitionId);
    }

    private String getCheckpointKey(String fullyQualifiedNamespace, String eventHubName, 
                                    String consumerGroup, String partitionId) {
        return String.format("%s/%s/%s/%s", fullyQualifiedNamespace, eventHubName, consumerGroup, partitionId);
    }
}
