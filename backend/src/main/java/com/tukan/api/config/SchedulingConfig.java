package com.tukan.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's annotation-driven scheduling. Kept separate from {@link AsyncConfig}
 * so async execution and scheduled tasks remain independent concerns.
 *
 * <p>Currently the only scheduled task is stuck-job recovery
 * (see {@code GenerationJobRecoveryService}).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
