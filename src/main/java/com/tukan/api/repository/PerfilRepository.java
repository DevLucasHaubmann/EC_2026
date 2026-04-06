package com.tukan.api.repository;

import com.tukan.api.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Integer> {

    boolean existsByUsuarioId(Integer usuarioId);

    Optional<Perfil> findByUsuarioId(Integer usuarioId);
}
