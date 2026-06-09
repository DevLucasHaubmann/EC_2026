package com.tukan.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables asynchronous execution and defines the dedicated executor used by the
 * asynchronous AI recommendation generation flow (Task 2.3D/2.3E).
 *
 * <p>A bounded pool with a bounded queue is intentional: AI generation is expensive
 * and must not be allowed to spawn unbounded threads. {@code CallerRunsPolicy} applies
 * backpressure instead of silently dropping jobs when the queue is saturated.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String AI_RECOMMENDATION_EXECUTOR = "aiRecommendationExecutor";

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 4;
    private static final int QUEUE_CAPACITY = 50;

    @Bean(name = AI_RECOMMENDATION_EXECUTOR)
    public Executor aiRecommendationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("ai-rec-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
