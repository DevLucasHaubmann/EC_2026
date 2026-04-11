package com.tukan.api.controller;

import com.tukan.api.dto.MeResponse;
import com.tukan.api.service.MeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;

    @GetMapping
    public ResponseEntity<MeResponse> getMe(Authentication authentication) {
        return ResponseEntity.ok(meService.getMe(authentication.getName()));
    }
}