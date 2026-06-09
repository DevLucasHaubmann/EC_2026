package com.tukan.api.controller;

import com.tukan.api.dto.bodyweight.BodyWeightLogResponse;
import com.tukan.api.dto.bodyweight.BodyWeightSummaryResponse;
import com.tukan.api.dto.bodyweight.RegisterBodyWeightRequest;
import com.tukan.api.service.BodyWeightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/me/body-weight")
@PreAuthorize("hasAuthority('USER')")
@RequiredArgsConstructor
public class BodyWeightController {

    private final BodyWeightService bodyWeightService;

    @PostMapping
    public ResponseEntity<BodyWeightLogResponse> register(
            @Valid @RequestBody RegisterBodyWeightRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(bodyWeightService.register(authentication.getName(), request.weightKg()));
    }

    @GetMapping
    public ResponseEntity<List<BodyWeightLogResponse>> getHistory(Authentication authentication) {
        return ResponseEntity.ok(bodyWeightService.getHistory(authentication.getName()));
    }

    @GetMapping("/summary")
    public ResponseEntity<BodyWeightSummaryResponse> getSummary(Authentication authentication) {
        return ResponseEntity.ok(bodyWeightService.getSummary(authentication.getName()));
    }
}
