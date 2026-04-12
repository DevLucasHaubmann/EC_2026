package com.tukan.api.repository;

import com.tukan.api.entity.FeedbackRecomendacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRecomendacaoRepository extends JpaRepository<FeedbackRecomendacao, Integer> {

    boolean existsByRecomendacaoId(Integer recomendacaoId);
}
