package com.tukan.api.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationJobStatusTest {

    @Test
    @DisplayName("Only COMPLETED and FAILED are terminal")
    void terminalStatesAreCompletedAndFailed() {
        // given / when / then
        assertThat(GenerationJobStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(GenerationJobStatus.FAILED.isTerminal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = GenerationJobStatus.class,
            names = {"PENDING", "PREPARING_CONTEXT", "GENERATING_WITH_AI", "SAVING_RECOMMENDATION"})
    @DisplayName("In-progress states are active and not terminal")
    void inProgressStatesAreActive(GenerationJobStatus status) {
        // given / when / then
        assertThat(status.isActive()).isTrue();
        assertThat(status.isTerminal()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(GenerationJobStatus.class)
    @DisplayName("isActive is always the negation of isTerminal")
    void activeIsAlwaysNegationOfTerminal(GenerationJobStatus status) {
        // given / when / then
        assertThat(status.isActive()).isEqualTo(!status.isTerminal());
    }

    @Test
    @DisplayName("activeStatuses contains exactly the statuses where isActive() is true")
    void activeStatusesMatchesIsActiveInvariant() {
        // given
        List<GenerationJobStatus> expected = Arrays.stream(GenerationJobStatus.values())
                .filter(GenerationJobStatus::isActive)
                .toList();

        // when / then
        assertThat(GenerationJobStatus.activeStatuses())
                .containsExactlyInAnyOrderElementsOf(expected)
                .doesNotContain(GenerationJobStatus.COMPLETED, GenerationJobStatus.FAILED);
    }
}
