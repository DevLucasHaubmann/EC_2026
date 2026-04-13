package com.tukan.api.controller;

import com.tukan.api.dto.UpdateUserRequest;
import com.tukan.api.dto.UserResponse;
import com.tukan.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> findAll(Pageable pageable) {
        Page<UserResponse> users = userService.findAll(pageable)
                .map(UserResponse::from);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(UserResponse.from(userService.findById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<UserResponse> findByEmail(@RequestParam String email) {
        return ResponseEntity.ok(UserResponse.from(userService.findByEmail(email)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Integer id,
                                               @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(UserResponse.from(userService.update(id, request)));
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id, Authentication authentication) {
        userService.delete(id, authentication.getName());
    }

    @DeleteMapping("/{id}/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSessions(@PathVariable Integer id) {
        userService.revokeAllSessions(id);
    }
}
