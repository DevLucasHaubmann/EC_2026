package com.tukan.api.domain.feedback;

import java.util.List;

public record FeedbackAiContext(
        List<FeedbackPreferenceSignal> preferences,
        List<FeedbackAvoidanceSignal> avoidances
) {
    public static FeedbackAiContext empty() {
        return new FeedbackAiContext(List.of(), List.of());
    }

    public boolean isEmpty() {
        return (preferences == null || preferences.isEmpty())
                && (avoidances == null || avoidances.isEmpty());
    }
}
