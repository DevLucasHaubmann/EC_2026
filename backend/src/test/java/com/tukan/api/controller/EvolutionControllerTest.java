package com.tukan.api.controller;

import com.tukan.api.dto.evolution.DailyEvolutionMetrics;
import com.tukan.api.dto.evolution.WeeklyEvolutionSummary;
import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.service.EvolutionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvolutionController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
class EvolutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvolutionService evolutionService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    private static final String EMAIL = "user@example.com";
    private static final LocalDate DATE = LocalDate.of(2026, 6, 8);
    private static final LocalDate WEEK_START = LocalDate.of(2026, 6, 1);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 6, 7);

    @Nested
    @DisplayName("GET /evolution/daily")
    class GetDaily {

        @Test
        @DisplayName("should return 200 with a single DailyEvolutionMetrics object (not array)")
        void shouldReturn200WithSingleObject() throws Exception {
            DailyEvolutionMetrics metrics = new DailyEvolutionMetrics(
                    DATE, 500.0, 30.0, 60.0, 15.0, 2, 3, 66.7);
            when(evolutionService.getDailyMetrics(anyString(), eq(DATE), eq(DATE)))
                    .thenReturn(List.of(metrics));

            mockMvc.perform(get("/evolution/daily")
                            .param("date", "2026-06-08")
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.date").value("2026-06-08"))
                    .andExpect(jsonPath("$.completedMeals").value(2))
                    .andExpect(jsonPath("$.plannedMeals").value(3))
                    .andExpect(jsonPath("$.adherencePercentage").value(66.7));
        }

        @Test
        @DisplayName("should return 200 with null adherencePercentage when no logs exist")
        void shouldReturn200WithNullAdherence() throws Exception {
            DailyEvolutionMetrics metrics = new DailyEvolutionMetrics(
                    DATE, 0.0, 0.0, 0.0, 0.0, 0, 0, null);
            when(evolutionService.getDailyMetrics(anyString(), eq(DATE), eq(DATE)))
                    .thenReturn(List.of(metrics));

            mockMvc.perform(get("/evolution/daily")
                            .param("date", "2026-06-08")
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.date").value("2026-06-08"))
                    .andExpect(jsonPath("$.completedMeals").value(0))
                    .andExpect(jsonPath("$.plannedMeals").value(0))
                    .andExpect(jsonPath("$.adherencePercentage").doesNotExist());
        }

        @Test
        @DisplayName("should return 400 when date param is missing")
        void shouldReturn400WhenDateMissing() throws Exception {
            mockMvc.perform(get("/evolution/daily")
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/evolution/daily")
                            .param("date", "2026-06-08"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("GET /evolution/weekly")
    class GetWeekly {

        @Test
        @DisplayName("should return 200 with WeeklyEvolutionSummary")
        void shouldReturn200WithWeeklySummary() throws Exception {
            DailyEvolutionMetrics day = new DailyEvolutionMetrics(
                    WEEK_START, 500.0, 30.0, 60.0, 15.0, 2, 3, 66.7);
            WeeklyEvolutionSummary summary = new WeeklyEvolutionSummary(
                    WEEK_START, WEEK_END, List.of(day), 75.0, 2500.0, 3);
            when(evolutionService.getSummary(anyString(), eq(WEEK_START)))
                    .thenReturn(summary);

            mockMvc.perform(get("/evolution/weekly")
                            .param("weekStart", "2026-06-01")
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.weekStart").value("2026-06-01"))
                    .andExpect(jsonPath("$.weekEnd").value("2026-06-07"))
                    .andExpect(jsonPath("$.activeDays").value(3))
                    .andExpect(jsonPath("$.totalCalories").value(2500.0))
                    .andExpect(jsonPath("$.averageAdherencePercentage").value(75.0));
        }

        @Test
        @DisplayName("should return 400 when weekStart param is missing")
        void shouldReturn400WhenWeekStartMissing() throws Exception {
            mockMvc.perform(get("/evolution/weekly")
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/evolution/weekly")
                            .param("weekStart", "2026-06-01"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
