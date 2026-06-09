package com.tukan.api.controller;

import com.tukan.api.dto.MeResponse;
import com.tukan.api.dto.UpdateAvatarRequest;
import com.tukan.api.service.MeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;

    @GetMapping
    public ResponseEntity<MeResponse> getMe(Authentication authentication) {
        var data = meService.findAuthenticatedUserData(authentication.getName());
        return ResponseEntity.ok(MeResponse.from(data));
    }

    @PreAuthorize("hasAuthority('USER')")
    @PatchMapping("/avatar")
    public ResponseEntity<MeResponse> updateAvatar(
            @Valid @RequestBody UpdateAvatarRequest request,
            Authentication authentication) {
        var data = meService.updateAvatar(authentication.getName(), request.avatarUrl());
        return ResponseEntity.ok(MeResponse.from(data));
    }
}
