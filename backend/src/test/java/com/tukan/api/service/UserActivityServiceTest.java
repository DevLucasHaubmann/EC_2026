package com.tukan.api.service;

import com.tukan.api.dto.activity.ActivityStreakResponse;
import com.tukan.api.entity.User;
import com.tukan.api.entity.UserActiveDay;
import com.tukan.api.repository.UserActiveDayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock
    private UserActiveDayRepository userActiveDayRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserActivityService userActivityService;

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
    // recordAccessAndGetStreak — persistence behaviour
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("recordAccessAndGetStreak — persistência")
    class RecordAccessTests {

        @Test
        @DisplayName("registra hoje quando o dia ainda não está presente")
        void recordAccess_todayAbsent_savesActiveDayAndReturnsStreak() {
            // given
            LocalDate today = LocalDate.now();
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(userActiveDayRepository.existsByUserIdAndActiveDate(USER_ID, today)).thenReturn(false);
            when(userActiveDayRepository.findActiveDatesByUserId(USER_ID)).thenReturn(List.of(today));
            when(userActiveDayRepository.save(any(UserActiveDay.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            ActivityStreakResponse response = userActivityService.recordAccessAndGetStreak(EMAIL);

            // then
            ArgumentCaptor<UserActiveDay> captor = ArgumentCaptor.forClass(UserActiveDay.class);
            verify(userActiveDayRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(user);
            assertThat(captor.getValue().getActiveDate()).isEqualTo(today);

            assertThat(response.currentStreak()).isGreaterThanOrEqualTo(1);
            assertThat(response.totalActiveDays()).isEqualTo(1);
            assertThat(response.lastActiveDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("idempotente: não salva quando hoje já está presente")
        void recordAccess_todayAlreadyPresent_doesNotSave() {
            // given
            LocalDate today = LocalDate.now();
            when(userService.findByEmail(EMAIL)).thenReturn(user);
            when(userActiveDayRepository.existsByUserIdAndActiveDate(USER_ID, today)).thenReturn(true);
            when(userActiveDayRepository.findActiveDatesByUserId(USER_ID)).thenReturn(List.of(today));

            // when
            userActivityService.recordAccessAndGetStreak(EMAIL);

            // then
            verify(userActiveDayRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // computeStreak — lógica determinística com reference fixo
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("computeStreak — lógica de cálculo")
    class ComputeStreakTests {

        private static final LocalDate REF = LocalDate.of(2026, 6, 9); // Monday

        @Test
        @DisplayName("lista vazia retorna zeros e null")
        void computeStreak_emptyList_returnsAllZeros() {
            // given / when
            ActivityStreakResponse result = userActivityService.computeStreak(List.of(), REF);

            // then
            assertThat(result.currentStreak()).isEqualTo(0);
            assertThat(result.longestStreak()).isEqualTo(0);
            assertThat(result.totalActiveDays()).isEqualTo(0);
            assertThat(result.lastActiveDate()).isNull();
        }

        @Test
        @DisplayName("só hoje: current=1, longest=1, total=1, last=hoje")
        void computeStreak_onlyToday_returnsOne() {
            // given
            List<LocalDate> dates = List.of(REF);

            // when
            ActivityStreakResponse result = userActivityService.computeStreak(dates, REF);

            // then
            assertThat(result.currentStreak()).isEqualTo(1);
            assertThat(result.longestStreak()).isEqualTo(1);
            assertThat(result.totalActiveDays()).isEqualTo(1);
            assertThat(result.lastActiveDate()).isEqualTo(REF);
        }

        @Test
        @DisplayName("sequência consecutiva terminando hoje retorna current correto")
        void computeStreak_consecutiveEndingToday_returnsCurrentStreak() {
            // given — hoje, -1, -2 (3 dias consecutivos)
            List<LocalDate> dates = List.of(
                    REF,
                    REF.minusDays(1),
                    REF.minusDays(2)
            );

            // when
            ActivityStreakResponse result = userActivityService.computeStreak(dates, REF);

            // then
            assertThat(result.currentStreak()).isEqualTo(3);
            assertThat(result.longestStreak()).isEqualTo(3);
            assertThat(result.totalActiveDays()).isEqualTo(3);
            assertThat(result.lastActiveDate()).isEqualTo(REF);
        }

        @Test
        @DisplayName("gap quebra current mas não longest quando histórico é maior")
        void computeStreak_gapBreaksCurrentStreak() {
            // given — hoje, -1, gap, -3 (current=2, longest=2, total=3)
            List<LocalDate> dates = List.of(
                    REF,
                    REF.minusDays(1),
                    REF.minusDays(3)
            );

            // when
            ActivityStreakResponse result = userActivityService.computeStreak(dates, REF);

            // then
            assertThat(result.currentStreak()).isEqualTo(2);
            assertThat(result.longestStreak()).isEqualTo(2);
            assertThat(result.totalActiveDays()).isEqualTo(3);
        }

        @Test
        @DisplayName("streak terminando ontem é mantido (reference não presente mas ontem presente)")
        void computeStreak_referenceAbsentYesterdayPresent_countsStreakFromYesterday() {
            // given — ontem e anteontem presentes, hoje não (usuário ainda não acessou hoje)
            LocalDate yesterday = REF.minusDays(1);
            LocalDate dayBefore = REF.minusDays(2);
            List<LocalDate> dates = List.of(yesterday, dayBefore);

            // when
            ActivityStreakResponse result = userActivityService.computeStreak(dates, REF);

            // then — streak ancora em ontem e conta para trás
            assertThat(result.currentStreak()).isEqualTo(2);
            assertThat(result.longestStreak()).isEqualTo(2);
            assertThat(result.totalActiveDays()).isEqualTo(2);
            assertThat(result.lastActiveDate()).isEqualTo(yesterday);
        }

        @Test
        @DisplayName("longest maior que current quando sequência antiga é maior")
        void computeStreak_longestBiggerThanCurrent() {
            // given — sequência antiga: -10, -9, -8, -7 (4 dias); atual: hoje, -1 (2 dias)
            List<LocalDate> dates = List.of(
                    REF,
                    REF.minusDays(1),
                    REF.minusDays(7),
                    REF.minusDays(8),
                    REF.minusDays(9),
                    REF.minusDays(10)
            );

            // when
            ActivityStreakResponse result = userActivityService.computeStreak(dates, REF);

            // then
            assertThat(result.currentStreak()).isEqualTo(2);
            assertThat(result.longestStreak()).isEqualTo(4);
            assertThat(result.totalActiveDays()).isEqualTo(6);
            assertThat(result.lastActiveDate()).isEqualTo(REF);
        }
    }
}
