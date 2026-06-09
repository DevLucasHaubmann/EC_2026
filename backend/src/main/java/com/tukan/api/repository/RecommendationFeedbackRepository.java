package com.tukan.api.repository;

import com.tukan.api.entity.RecommendationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Integer> {

    boolean existsByRecommendationId(Integer recommendationId);

    Optional<RecommendationFeedback> findByRecommendationId(Integer recommendationId);

    @Modifying(clearAutomatically = true)
    @Query("delete from RecommendationFeedback f where f.recommendation.user.id = :userId")
    void deleteByRecommendationUserId(@Param("userId") Integer userId);
}
