package com.tukan.api.repository;

import com.tukan.api.entity.UserSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserSession s JOIN FETCH s.usuario WHERE s.refreshTokenHash = :hash")
    Optional<UserSession> findByRefreshTokenHash(@Param("hash") String hash);

    @Modifying
    @Query("UPDATE UserSession s SET s.revogadoEm = :now WHERE s.usuario.id = :usuarioId AND s.revogadoEm IS NULL")
    int revokeAllByUsuarioId(@Param("usuarioId") Integer usuarioId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.usuario.id = :usuarioId")
    void deleteAllByUsuarioId(@Param("usuarioId") Integer usuarioId);
}
