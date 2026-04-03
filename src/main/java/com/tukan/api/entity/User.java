package com.tukan.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    public enum UserType {
        ADMIN,
        USER
    }

    public enum UserState {
        ATIVO,
        BLOQUEADO,
        BANIDO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 100)
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    private String email;

    @Column(name = "senha", nullable = false, length = 255)
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private UserType tipo = UserType.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserState status = UserState.ATIVO;
}