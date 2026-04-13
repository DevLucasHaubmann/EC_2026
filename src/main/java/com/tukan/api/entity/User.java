package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    public enum UserType {
        ADMIN,
        USER
    }

    public enum UserState {
        ACTIVE,
        BLOCKED,
        BANNED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "senha", nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private UserType type = UserType.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserState status = UserState.ACTIVE;
}
