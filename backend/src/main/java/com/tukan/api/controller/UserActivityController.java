package com.tukan.api.controller;

import com.tukan.api.dto.activity.ActivityStreakResponse;
import com.tukan.api.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/activity")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
public class UserActivityController {

    private final UserActivityService userActivityService;

    @PostMapping
    public ResponseEntity<ActivityStreakResponse> recordAccess(Authentication authentication) {
        return ResponseEntity.ok(userActivityService.recordAccessAndGetStreak(authentication.getName()));
    }
}
