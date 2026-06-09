package com.tukan.api.dto;

import com.tukan.api.entity.GenerationJob;
import com.tukan.api.entity.GenerationJobStatus;

import java.util.UUID;

/**
 * Single API representation of a {@link GenerationJob}, shared by the start-job response,
 * the status-fallback endpoint and the SSE event payload — all three carry the same shape.
 *
 * <p>{@code recommendationId} is only populated on {@code COMPLETED}; {@code errorMessage}
 * only on {@code FAILED}. No fake progress percentage and no time estimate are exposed.
 */
public record GenerationJobResponse(
        UUID jobId,
        GenerationJobStatus status,
        Integer recommendationId,
        String errorMessage
) {

    public static GenerationJobResponse from(GenerationJob job) {
        return new GenerationJobResponse(
                job.getId(),
                job.getStatus(),
                job.getRecommendationId(),
                job.getErrorMessage());
    }
}
