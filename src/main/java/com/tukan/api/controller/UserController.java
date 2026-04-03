package com.tukan.api.controller;

import com.tukan.api.dto.UpdateUserRequest;
import com.tukan.api.dto.UserResponse;
import com.tukan.api.entity.User;
import com.tukan.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAllUsers(){
        List<UserResponse> users = userService.findAll().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.OK).body(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Integer id){
        Optional<User> user = userService.findById(id);

        if(user.isPresent()){
            return ResponseEntity.status(HttpStatus.OK).body(UserResponse.from(user.get()));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");
    }

    @GetMapping("/search")
    public ResponseEntity<?> findByEmail(@RequestParam String email){
        Optional<User> user = userService.findByEmail(email);

        if(user.isPresent()){
            return ResponseEntity.status(HttpStatus.OK).body(UserResponse.from(user.get()));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Integer id,
                                               @RequestBody @Valid UpdateUserRequest request) {
        User user = userService.update(id, request);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}