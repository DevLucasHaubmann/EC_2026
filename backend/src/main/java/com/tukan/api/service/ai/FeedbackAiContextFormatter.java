package com.tukan.api.service.ai;

import com.tukan.api.domain.feedback.FeedbackAiContext;
import com.tukan.api.domain.feedback.FeedbackAvoidanceSignal;
import com.tukan.api.domain.feedback.FeedbackPreferenceSignal;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public final class FeedbackAiContextFormatter {

    static final int MAX_LABEL_LENGTH = 80;
    static final int MAX_SIGNALS_PER_CATEGORY = 5;
    static final int MAX_OUTPUT_LENGTH = 400;

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1F\\x7F]");
    private static final Pattern EXTRA_SPACES = Pattern.compile("\\s{2,}");

    public String format(FeedbackAiContext context) {
        if (context == null || context.isEmpty()) {
            return "";
        }

        List<String> prefLabels = sanitizedLabels(
                context.preferences() == null ? null :
                context.preferences().stream().map(FeedbackPreferenceSignal::label).collect(Collectors.toList()));
        List<String> avoidLabels = sanitizedLabels(
                context.avoidances() == null ? null :
                context.avoidances().stream().map(FeedbackAvoidanceSignal::label).collect(Collectors.toList()));

        if (prefLabels.isEmpty() && avoidLabels.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        if (!prefLabels.isEmpty()) {
            sb.append("Preferências observadas do usuário: ")
              .append(String.join(", ", prefLabels))
              .append(".");
        }
        if (!avoidLabels.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("Evitar com base em feedback anterior: ")
              .append(String.join(", ", avoidLabels))
              .append(".");
        }

        return truncate(sb.toString(), MAX_OUTPUT_LENGTH);
    }

    private List<String> sanitizedLabels(List<String> rawLabels) {
        if (rawLabels == null) {
            return List.of();
        }
        return rawLabels.stream()
                .filter(l -> l != null && !l.isBlank())
                .map(this::sanitizeLabel)
                .filter(l -> !l.isEmpty())
                .limit(MAX_SIGNALS_PER_CATEGORY)
                .collect(Collectors.toList());
    }

    private String sanitizeLabel(String label) {
        String cleaned = CONTROL_CHARS.matcher(label).replaceAll(" ");
        cleaned = EXTRA_SPACES.matcher(cleaned).replaceAll(" ").strip();
        if (cleaned.length() > MAX_LABEL_LENGTH) {
            return cleaned.substring(0, MAX_LABEL_LENGTH).strip();
        }
        return cleaned;
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
