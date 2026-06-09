package com.tukan.api.util;

import java.util.regex.Pattern;

/**
 * Sanitizes values before they reach the logs.
 *
 * <p>External-facing strings (provider error messages, AI payloads) must never be logged verbatim:
 * they may carry newlines that break log parsing, be unbounded in length, or echo sensitive input.
 * {@link #sanitize(String)} collapses whitespace and truncates; {@link #fingerprint(String)} yields a
 * non-reversible length+hash signature so identical prompts can be correlated without exposing content.
 */
public final class LogSanitizer {

    private static final int DEFAULT_MAX_LENGTH = 200;
    private static final String TRUNCATION_MARKER = "...";
    private static final String EMPTY_PLACEHOLDER = "n/a";
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private LogSanitizer() {
    }

    /** Collapses whitespace and truncates to the default length. Never returns {@code null}. */
    public static String sanitize(String value) {
        return sanitize(value, DEFAULT_MAX_LENGTH);
    }

    /**
     * Collapses internal whitespace (including newlines) into single spaces and truncates the result
     * to {@code maxLength}, appending an ellipsis when content is dropped. Null/blank input becomes a
     * fixed placeholder so logs stay parseable.
     */
    public static String sanitize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return EMPTY_PLACEHOLDER;
        }
        String collapsed = WHITESPACE.matcher(value).replaceAll(" ").trim();
        if (collapsed.length() <= maxLength) {
            return collapsed;
        }
        return collapsed.substring(0, maxLength) + TRUNCATION_MARKER;
    }

    /**
     * Non-reversible signature of a sensitive string (e.g. an AI prompt). Exposes only the length and a
     * hash so the value can be correlated across log lines without leaking its content.
     */
    public static String fingerprint(String value) {
        if (value == null || value.isEmpty()) {
            return "len=0";
        }
        return "len=" + value.length() + ",hash=" + Integer.toHexString(value.hashCode());
    }
}
