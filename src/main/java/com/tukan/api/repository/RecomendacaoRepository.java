package com.tukan.api.repository;

import com.tukan.api.entity.Recomendacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

public interface RecomendacaoRepository extends JpaRepository<Recomendacao, Integer> {

    @EntityGraph(attributePaths = "usuario")
    Optional<Recomendacao> findFirstByUsuarioIdOrderByCriadoEmDesc(Integer usuarioId);

    @EntityGraph(attributePaths = "usuario")
    List<Recomendacao> findByUsuarioIdOrderByCriadoEmDesc(Integer usuarioId);

    @Override
    @EntityGraph(attributePaths = "usuario")
    @NonNull
    Optional<Recomendacao> findById(@NonNull Integer id);
}
