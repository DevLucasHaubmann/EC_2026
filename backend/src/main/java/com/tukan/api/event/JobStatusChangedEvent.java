package com.tukan.api.event;

import com.tukan.api.entity.GenerationJobStatus;

import java.util.UUID;

/**
 * Domain event published whenever a generation job changes status. It is a plain value
 * snapshot — not an SSE concern — so the publisher ({@code GenerationJobService}) stays
 * unaware of streaming. Consumed after transaction commit by the SSE layer.
 */
public record JobStatusChangedEvent(
        UUID jobId,
        GenerationJobStatus status,
        Integer recommendationId,
        String errorMessage
) {
}
