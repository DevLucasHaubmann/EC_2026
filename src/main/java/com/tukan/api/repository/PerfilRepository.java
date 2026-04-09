package com.tukan.api.repository;

import com.tukan.api.entity.Perfil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Integer> {

    boolean existsByUsuarioId(Integer usuarioId);

    @EntityGraph(attributePaths = "usuario")
    Optional<Perfil> findByUsuarioId(Integer usuarioId);

    @Override
    @EntityGraph(attributePaths = "usuario")
    @NonNull
    Optional<Perfil> findById(@NonNull Integer id);

    @Override
    @EntityGraph(attributePaths = "usuario")
    @NonNull
    Page<Perfil> findAll(@NonNull Pageable pageable);

    void deleteByUsuarioId(Integer usuarioId);
}
