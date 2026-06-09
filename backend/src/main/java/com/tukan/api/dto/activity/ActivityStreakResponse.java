package com.tukan.api.dto.activity;

import java.time.LocalDate;

public record ActivityStreakResponse(
        int currentStreak,
        int longestStreak,
        int totalActiveDays,
        LocalDate lastActiveDate
) {}
