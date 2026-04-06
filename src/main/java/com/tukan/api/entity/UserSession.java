package com.tukan.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessao_usuario", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "refresh_token_hash"),
        @Index(name = "idx_usuario_id", columnList = "usuario_id")
})
@Getter
@Setter
@NoArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @Column(name = "ultimo_uso_em", nullable = false)
    private Instant ultimoUsoEm;

    @Column(name = "dispositivo", length = 255)
    private String dispositivo;

    @Column(name = "endereco_ip", length = 45)
    private String enderecoIp;

    public boolean isExpired() {
        return Instant.now().isAfter(expiraEm);
    }

    public boolean isRevoked() {
        return revogadoEm != null;
    }

    public boolean isActive() {
        return !isRevoked() && !isExpired();
    }
}
