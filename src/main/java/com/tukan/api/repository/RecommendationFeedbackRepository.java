package com.tukan.api.repository;

import com.tukan.api.entity.RecommendationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Integer> {

    boolean existsByRecommendationId(Integer recommendationId);
}