package com.tukan.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukan.api.dto.meallog.LogMealRequest;
import com.tukan.api.dto.meallog.MealLogResponse;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.exception.GlobalExceptionHandler;
import com.tukan.api.security.DatabaseBackedJwtAuthenticationConverter;
import com.tukan.api.service.MealLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MealLogController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
class MealLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MealLogService mealLogService;

    @MockitoBean
    private DatabaseBackedJwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EMAIL = "user@example.com";
    private static final LocalDate DATE = LocalDate.of(2026, 6, 8);

    private static final MealLogResponse MEAL_LOG_RESPONSE = new MealLogResponse(
            100L, 10, DATE, "BREAKFAST", 1, 380.0, 25.0, 40.0, 10.0, null, Instant.parse("2026-06-08T08:00:00Z"));

    @Nested
    @DisplayName("POST /meal-logs")
    class LogMeal {

        @Test
        @DisplayName("should return 201 with logged meal on success")
        void shouldReturn201OnSuccess() throws Exception {
            LogMealRequest request = new LogMealRequest(10, DATE, "BREAKFAST", 1, null);
            when(mealLogService.logMeal(anyString(), any(LogMealRequest.class)))
                    .thenReturn(MEAL_LOG_RESPONSE);

            mockMvc.perform(post("/meal-logs")
                            .with(jwt().jwt(j -> j.subject(EMAIL)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.mealType").value("BREAKFAST"))
                    .andExpect(jsonPath("$.calories").value(380.0));
        }

        @Test
        @DisplayName("should return 400 when recommendationId is missing (Bean Validation)")
        void shouldReturn400WhenRecommendationIdMissing() throws Exception {
            String body = """
                    {
                        "mealDate": "2026-06-08",
                        "mealType": "BREAKFAST",
                        "optionNumber": 1
                    }
                    """;

            mockMvc.perform(post("/meal-logs")
                            .with(jwt().jwt(j -> j.subject(EMAIL)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 409 when service throws BusinessException CONFLICT")
        void shouldReturn409OnConflict() throws Exception {
            LogMealRequest request = new LogMealRequest(10, DATE, "BREAKFAST", 1, null);
            when(mealLogService.logMeal(anyString(), any(LogMealRequest.class)))
                    .thenThrow(new BusinessException("Esta refeição já foi registrada.", HttpStatus.CONFLICT));

            mockMvc.perform(post("/meal-logs")
                            .with(jwt().jwt(j -> j.subject(EMAIL)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 403 when service throws BusinessException FORBIDDEN")
        void shouldReturn403OnForbidden() throws Exception {
            LogMealRequest request = new LogMealRequest(10, DATE, "BREAKFAST", 1, null);
            when(mealLogService.logMeal(anyString(), any(LogMealRequest.class)))
                    .thenThrow(new BusinessException("Acesso negado.", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/meal-logs")
                            .with(jwt().jwt(j -> j.subject(EMAIL)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(post("/meal-logs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("GET /meal-logs")
    class GetByDate {

        @Test
        @DisplayName("should return 200 with array of meal logs")
        void shouldReturn200WithArray() throws Exception {
            when(mealLogService.getLogsByDate(anyString(), eq(DATE)))
                    .thenReturn(List.of(MEAL_LOG_RESPONSE));

            mockMvc.perform(get("/meal-logs")
                            .param("date", "2026-06-08")
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(100))
                    .andExpect(jsonPath("$[0].mealType").value("BREAKFAST"));
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(get("/meal-logs")
                            .param("date", "2026-06-08"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("DELETE /meal-logs/{id}")
    class DeleteLog {

        @Test
        @DisplayName("should return 204 on successful deletion")
        void shouldReturn204OnSuccess() throws Exception {
            mockMvc.perform(delete("/meal-logs/1")
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when service throws BusinessException NOT_FOUND")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(new BusinessException("Registro não encontrado.", HttpStatus.NOT_FOUND))
                    .when(mealLogService).deleteLog(anyString(), eq(99L));

            mockMvc.perform(delete("/meal-logs/99")
                            .with(jwt().jwt(j -> j.subject(EMAIL))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should reject unauthenticated request")
        void shouldRejectUnauthenticated() throws Exception {
            mockMvc.perform(delete("/meal-logs/1"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
