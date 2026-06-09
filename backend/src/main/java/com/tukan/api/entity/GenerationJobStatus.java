package com.tukan.api.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discrete, real states of an asynchronous AI recommendation generation job.
 * No fake percentage — only meaningful, observable phases (see Task 2.3B).
 */
public enum GenerationJobStatus {

    PENDING,
    PREPARING_CONTEXT,
    GENERATING_WITH_AI,
    SAVING_RECOMMENDATION,
    COMPLETED,
    FAILED;

    private static final Set<GenerationJobStatus> TERMINAL = Set.of(COMPLETED, FAILED);

    private static final Set<GenerationJobStatus> ACTIVE = Arrays.stream(values())
            .filter(GenerationJobStatus::isActive)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * Terminal states cannot transition any further.
     */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /**
     * Active states represent a job still in progress.
     */
    public boolean isActive() {
        return !isTerminal();
    }

    /**
     * Single source of truth for the set of non-terminal statuses. Recovery/cleanup
     * derive their query filter from this instead of duplicating a fragile literal list,
     * so a newly added active status is covered automatically.
     */
    public static Set<GenerationJobStatus> activeStatuses() {
        return ACTIVE;
    }
}
