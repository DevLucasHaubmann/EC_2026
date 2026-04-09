package com.tukan.api.repository;

import com.tukan.api.entity.Triagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface TriagemRepository extends JpaRepository<Triagem, Integer> {

    boolean existsByUsuarioId(Integer usuarioId);

    @EntityGraph(attributePaths = "usuario")
    Optional<Triagem> findByUsuarioId(Integer usuarioId);

    @Override
    @EntityGraph(attributePaths = "usuario")
    @NonNull
    Optional<Triagem> findById(@NonNull Integer id);

    @Override
    @EntityGraph(attributePaths = "usuario")
    @NonNull
    Page<Triagem> findAll(@NonNull Pageable pageable);

    void deleteByUsuarioId(Integer usuarioId);
}
