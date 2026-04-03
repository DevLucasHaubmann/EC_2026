package com.tukan.api.service;

import com.tukan.api.dto.UpdateUserRequest;
import com.tukan.api.entity.User;
import com.tukan.api.exception.BusinessException;
import com.tukan.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public List<User> findAll(){
        return userRepository.findAll();
    }

    public Optional<User> findById(Integer id){
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email){
        return userRepository.findByEmail(authService.normalizeEmail(email));
    }

    public User update(Integer id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        if (request.nome() != null && !request.nome().isBlank()) {
            user.setNome(request.nome().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String normalizedEmail = authService.normalizeEmail(request.email());
            if (!normalizedEmail.equals(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
                throw new BusinessException("E-mail já cadastrado.");
            }
            user.setEmail(normalizedEmail);
        }

        if (request.tipo() != null) {
            user.setTipo(request.tipo());
        }

        if (request.status() != null) {
            user.setStatus(request.status());
        }

        return userRepository.save(user);
    }

    public void delete(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException("Usuário não encontrado.");
        }
        userRepository.deleteById(id);
    }
}