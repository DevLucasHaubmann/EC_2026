package com.tukan.api.service;

import com.tukan.api.dto.bodyweight.BodyWeightLogResponse;
import com.tukan.api.dto.bodyweight.BodyWeightSummaryResponse;
import com.tukan.api.entity.BodyWeightLog;
import com.tukan.api.entity.User;
import com.tukan.api.repository.BodyWeightLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BodyWeightServiceTest {

    @Mock
    private BodyWeightLogRepository bodyWeightLogRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private BodyWeightService bodyWeightService;

    private static final String EMAIL = "user@example.com";
    private static final Integer USER_ID = 1;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BodyWeightLog buildLog(Long id, BigDecimal weight, LocalDate date) {
        BodyWeightLog log = new BodyWeightLog();
        log.setId(id);
        log.setUser(user);
        log.setWeightKg(weight);
        log.setMeasuredDate(date);
        // simulate @PrePersist not running in unit tests
        try {
            var field = BodyWeightLog.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(log, Instant.now());
        } catch (Exception ignored) {
        }
        return log;
    }

    // -------------------------------------------------------------------------
    // register — append-only history (no upsert)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("register — append-only")
    class RegisterTests {

        @Test
        @DisplayName("cria um novo registro a cada chamada para o usuário atual")
        void register_alwaysCreatesNewEntry() {
            // given
            BigDecimal weight = new BigDecimal("75.50");
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(bodyWeightLogRepository.save(any(BodyWeightLog.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // when
            BodyWeightLogResponse response = bodyWeightService.register(EMAIL, weight);

            // then — a brand new log (id still null before persistence) is saved
            ArgumentCaptor<BodyWeightLog> captor = ArgumentCaptor.forClass(BodyWeightLog.class);
            verify(bodyWeightLogRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getId()).isNull();
            assertThat(captor.getValue().getUser()).isEqualTo(user);
            assertThat(captor.getValue().getWeightKg()).isEqualByComparingTo(weight);
            assertThat(captor.getValue().getMeasuredDate()).isEqualTo(LocalDate.now());
            assertThat(response.weightKg()).isEqualByComparingTo(weight);
        }

        @Test
        @DisplayName("dois registros no mesmo dia geram dois logs distintos (sem substituição)")
        void register_twiceSameDay_createsTwoDistinctEntries() {
            // given
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(bodyWeightLogRepository.save(any(BodyWeightLog.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // when — the user records 90 kg and later 89 kg on the same day
            bodyWeightService.register(EMAIL, new BigDecimal("90.00"));
            bodyWeightService.register(EMAIL, new BigDecimal("89.00"));

            // then — two separate new entities are persisted; the second never reuses the first
            ArgumentCaptor<BodyWeightLog> captor = ArgumentCaptor.forClass(BodyWeightLog.class);
            verify(bodyWeightLogRepository, times(2)).save(captor.capture());
            List<BodyWeightLog> saved = captor.getAllValues();
            assertThat(saved).hasSize(2);
            assertThat(saved.get(0)).isNotSameAs(saved.get(1));
            assertThat(saved.get(0).getWeightKg()).isEqualByComparingTo(new BigDecimal("90.00"));
            assertThat(saved.get(1).getWeightKg()).isEqualByComparingTo(new BigDecimal("89.00"));
        }
    }

    // -------------------------------------------------------------------------
    // getHistory
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getHistory")
    class GetHistoryTests {

        @Test
        @DisplayName("retorna histórico em ordem cronológica ascendente")
        void getHistory_returnsOrderedAsc() {
            // given
            LocalDate d1 = LocalDate.of(2026, 1, 1);
            LocalDate d2 = LocalDate.of(2026, 2, 1);
            List<BodyWeightLog> logs = List.of(
                    buildLog(1L, new BigDecimal("80.00"), d1),
                    buildLog(2L, new BigDecimal("78.50"), d2)
            );
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(bodyWeightLogRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID)).thenReturn(logs);

            // when
            List<BodyWeightLogResponse> result = bodyWeightService.getHistory(EMAIL);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).measuredDate()).isEqualTo(d1);
            assertThat(result.get(1).measuredDate()).isEqualTo(d2);
        }

        @Test
        @DisplayName("retorna lista vazia quando não há registros")
        void getHistory_noLogs_returnsEmpty() {
            // given
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(bodyWeightLogRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID)).thenReturn(List.of());

            // when
            List<BodyWeightLogResponse> result = bodyWeightService.getHistory(EMAIL);

            // then
            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // getSummary — edge cases
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getSummary — casos extremos")
    class GetSummaryTests {

        @Test
        @DisplayName("0 registros: current/initial/totalChange null, trend null, entryCount 0")
        void getSummary_noLogs_returnsAllNulls() {
            // given
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(bodyWeightLogRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID)).thenReturn(List.of());

            // when
            BodyWeightSummaryResponse result = bodyWeightService.getSummary(EMAIL);

            // then
            assertThat(result.currentWeightKg()).isNull();
            assertThat(result.initialWeightKg()).isNull();
            assertThat(result.totalChangeKg()).isNull();
            assertThat(result.trend()).isNull();
            assertThat(result.entryCount()).isEqualTo(0);
            assertThat(result.history()).isEmpty();
        }

        @Test
        @DisplayName("1 registro: current=initial=peso, totalChange null, trend null")
        void getSummary_oneLog_currentEqualsInitialNoTrend() {
            // given
            BigDecimal weight = new BigDecimal("75.00");
            BodyWeightLog log = buildLog(1L, weight, LocalDate.of(2026, 1, 1));
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(bodyWeightLogRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID)).thenReturn(List.of(log));

            // when
            BodyWeightSummaryResponse result = bodyWeightService.getSummary(EMAIL);

            // then
            assertThat(result.currentWeightKg()).isEqualByComparingTo(weight);
            assertThat(result.initialWeightKg()).isEqualByComparingTo(weight);
            assertThat(result.totalChangeKg()).isNull();
            assertThat(result.trend()).isNull();
            assertThat(result.entryCount()).isEqualTo(1);
            assertThat(result.history()).hasSize(1);
        }

        @Test
        @DisplayName("trend LOSS quando peso atual < inicial")
        void getSummary_twoLogs_trendLoss() {
            // given
            BodyWeightLog first = buildLog(1L, new BigDecimal("80.00"), LocalDate.of(2026, 1, 1));
            BodyWeightLog last = buildLog(2L, new BigDecimal("75.00"), LocalDate.of(2026, 2, 1));
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(bodyWeightLogRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID))
                    .thenReturn(List.of(first, last));

            // when
            BodyWeightSummaryResponse result = bodyWeightService.getSummary(EMAIL);

            // then
            assertThat(result.trend()).isEqualTo("LOSS");
            assertThat(result.totalChangeKg()).isEqualByComparingTo(new BigDecimal("-5.00"));
            assertThat(result.currentWeightKg()).isEqualByComparingTo(new BigDecimal("75.00"));
            assertThat(result.initialWeightKg()).isEqualByComparingTo(new BigDecimal("80.00"));
            assertThat(result.entryCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("trend GAIN quando peso atual > inicial")
        void getSummary_twoLogs_trendGain() {
            // given
            BodyWeightLog first = buildLog(1L, new BigDecimal("70.00"), LocalDate.of(2026, 1, 1));
            BodyWeightLog last = buildLog(2L, new BigDecimal("75.00"), LocalDate.of(2026, 2, 1));
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(bodyWeightLogRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID))
                    .thenReturn(List.of(first, last));

            // when
            BodyWeightSummaryResponse result = bodyWeightService.getSummary(EMAIL);

            // then
            assertThat(result.trend()).isEqualTo("GAIN");
            assertThat(result.totalChangeKg()).isEqualByComparingTo(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("trend STABLE quando peso atual == inicial")
        void getSummary_twoLogs_trendStable() {
            // given
            BodyWeightLog first = buildLog(1L, new BigDecimal("72.00"), LocalDate.of(2026, 1, 1));
            BodyWeightLog last = buildLog(2L, new BigDecimal("72.00"), LocalDate.of(2026, 2, 1));
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(bodyWeightLogRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID))
                    .thenReturn(List.of(first, last));

            // when
            BodyWeightSummaryResponse result = bodyWeightService.getSummary(EMAIL);

            // then
            assertThat(result.trend()).isEqualTo("STABLE");
            assertThat(result.totalChangeKg()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("history inclui todos os pontos em ordem cronológica")
        void getSummary_multipleEntries_historyOrderedAsc() {
            // given
            LocalDate d1 = LocalDate.of(2026, 1, 1);
            LocalDate d2 = LocalDate.of(2026, 2, 1);
            LocalDate d3 = LocalDate.of(2026, 3, 1);
            List<BodyWeightLog> logs = List.of(
                    buildLog(1L, new BigDecimal("80.00"), d1),
                    buildLog(2L, new BigDecimal("78.00"), d2),
                    buildLog(3L, new BigDecimal("76.00"), d3)
            );
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(bodyWeightLogRepository.findByUserIdOrderByCreatedAtAscIdAsc(USER_ID)).thenReturn(logs);

            // when
            BodyWeightSummaryResponse result = bodyWeightService.getSummary(EMAIL);

            // then
            assertThat(result.entryCount()).isEqualTo(3);
            assertThat(result.history()).hasSize(3);
            assertThat(result.history().get(0).measuredDate()).isEqualTo(d1);
            assertThat(result.history().get(2).measuredDate()).isEqualTo(d3);
            assertThat(result.initialWeightKg()).isEqualByComparingTo(new BigDecimal("80.00"));
            assertThat(result.currentWeightKg()).isEqualByComparingTo(new BigDecimal("76.00"));
            assertThat(result.trend()).isEqualTo("LOSS");
        }
    }
}
