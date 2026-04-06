package com.tukan.api.repository;

import com.tukan.api.entity.Triagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TriagemRepository extends JpaRepository<Triagem, Integer> {

    boolean existsByUsuarioId(Integer usuarioId);

    Optional<Triagem> findByUsuarioId(Integer usuarioId);
}
