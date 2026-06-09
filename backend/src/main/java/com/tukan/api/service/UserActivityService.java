package com.tukan.api.service;

import com.tukan.api.dto.activity.ActivityStreakResponse;
import com.tukan.api.entity.User;
import com.tukan.api.entity.UserActiveDay;
import com.tukan.api.repository.UserActiveDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActiveDayRepository userActiveDayRepository;
    private final UserService userService;

    @Transactional
    public ActivityStreakResponse recordAccessAndGetStreak(String email) {
        User user = userService.findByEmail(email);
        LocalDate today = LocalDate.now();

        if (!userActiveDayRepository.existsByUserIdAndActiveDate(user.getId(), today)) {
            UserActiveDay activeDay = new UserActiveDay();
            activeDay.setUser(user);
            activeDay.setActiveDate(today);
            userActiveDayRepository.save(activeDay);
        }

        List<LocalDate> dates = userActiveDayRepository.findActiveDatesByUserId(user.getId());
        return computeStreak(dates, today);
    }

    /**
     * Package-private for deterministic unit testing without depending on LocalDate.now().
     *
     * @param activeDates all active dates for the user (order does not matter — sorted internally)
     * @param reference   the reference date (normally today) used to anchor the current streak
     */
    ActivityStreakResponse computeStreak(List<LocalDate> activeDates, LocalDate reference) {
        if (activeDates.isEmpty()) {
            return new ActivityStreakResponse(0, 0, 0, null);
        }

        // O(1) lookups for consecutive-day checks
        Set<LocalDate> dateSet = new HashSet<>(activeDates);

        int currentStreak = computeCurrentStreak(dateSet, reference);
        int longestStreak = computeLongestStreak(activeDates);
        int totalActiveDays = activeDates.size();
        LocalDate lastActiveDate = Collections.max(activeDates);

        return new ActivityStreakResponse(currentStreak, longestStreak, totalActiveDays, lastActiveDate);
    }

    /**
     * Anchors the current streak on {@code reference} if present; otherwise falls back to
     * {@code reference.minusDays(1)} — this ensures the streak is not broken before the user
     * registers today's access (the record call happens in the same transaction, but tests
     * need this fallback for the "yesterday" scenario).
     */
    private int computeCurrentStreak(Set<LocalDate> dateSet, LocalDate reference) {
        LocalDate anchor = resolveAnchor(dateSet, reference);
        if (anchor == null) {
            return 0;
        }

        int streak = 1;
        LocalDate cursor = anchor.minusDays(1);
        while (dateSet.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private LocalDate resolveAnchor(Set<LocalDate> dateSet, LocalDate reference) {
        if (dateSet.contains(reference)) {
            return reference;
        }
        LocalDate yesterday = reference.minusDays(1);
        if (dateSet.contains(yesterday)) {
            return yesterday;
        }
        return null;
    }

    /**
     * Single O(n) pass over dates sorted ascending to find the longest consecutive sequence.
     */
    private int computeLongestStreak(List<LocalDate> activeDates) {
        List<LocalDate> sorted = new ArrayList<>(activeDates);
        Collections.sort(sorted);

        int longest = 1;
        int current = 1;

        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                current++;
                if (current > longest) {
                    longest = current;
                }
            } else {
                current = 1;
            }
        }

        return longest;
    }
}
