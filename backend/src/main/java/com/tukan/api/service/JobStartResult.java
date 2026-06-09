package com.tukan.api.service;

import com.tukan.api.dto.GenerationJobResponse;

/**
 * Outcome of a start-job request. {@code created} distinguishes a freshly created job
 * (the caller must dispatch async generation, HTTP 202) from an already-active job
 * returned for future re-attachment (HTTP 409, no new async dispatch).
 *
 * <p>Carries a DTO, never the entity, so the entity does not leak past the service layer.
 */
public record JobStartResult(GenerationJobResponse job, boolean created) {

    public static JobStartResult created(GenerationJobResponse job) {
        return new JobStartResult(job, true);
    }

    public static JobStartResult alreadyActive(GenerationJobResponse job) {
        return new JobStartResult(job, false);
    }
}
