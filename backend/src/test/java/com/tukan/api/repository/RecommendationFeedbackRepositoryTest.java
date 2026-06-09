package com.tukan.api.repository;

import com.tukan.api.entity.Recommendation;
import com.tukan.api.entity.RecommendationFeedback;
import com.tukan.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence round-trip for RecommendationFeedback against a JPA-managed schema.
 *
 * Guards the entity/converter mapping that the mocked service and @WebMvcTest tests skip:
 * rating persisted as an integer, the free-text comment column, and the liked/disliked tag
 * lists serialized through FeedbackTagListConverter. It also exercises the JPQL delete query
 * UserDeletionService relies on, ensuring feedback created through the JPA layer does not block
 * user deletion.
 *
 * Note: the schema here is derived from the entity (H2), so this proves the entity is internally
 * consistent. Alignment with the live MySQL table is enforced by scripts/013 and verified at runtime.
 */
@DataJpaTest
class RecommendationFeedbackRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RecommendationFeedbackRepository feedbackRepository;

    private Recommendation recommendation;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("Lucas");
        user.setEmail("lucas@email.com");
        user.setPassword("hashed");
        user.setType(User.UserType.USER);
        user.setStatus(User.UserState.ACTIVE);
        entityManager.persist(user);

        recommendation = new Recommendation();
        recommendation.setUser(user);
        recommendation.setSummary("Plano semanal");
        recommendation.setPlanJson("{}");
        recommendation.setContextJson("{}");
        recommendation.setProvider("gemini");
        recommendation.setModel("gemini-3.1-flash-lite");
        recommendation.setStatus(Recommendation.RecommendationStatus.GENERATED);
        entityManager.persist(recommendation);

        entityManager.flush();
    }

    @Test
    @DisplayName("persists and reads back all feedback fields including rating, comment and tag lists")
    void shouldRoundTripFeedback() {
        // given
        RecommendationFeedback feedback = new RecommendationFeedback();
        feedback.setRecommendation(recommendation);
        feedback.setRating(4);
        feedback.setComment("Muito prático e variado");
        feedback.setLikedTags(List.of(
                RecommendationFeedback.FeedbackTag.PRACTICAL,
                RecommendationFeedback.FeedbackTag.VARIED));
        feedback.setDislikedTags(List.of(RecommendationFeedback.FeedbackTag.EXPENSIVE));

        // when
        feedbackRepository.save(feedback);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<RecommendationFeedback> reloaded = feedbackRepository.findByRecommendationId(recommendation.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getRating()).isEqualTo(4);
        assertThat(reloaded.get().getComment()).isEqualTo("Muito prático e variado");
        assertThat(reloaded.get().getLikedTags())
                .containsExactly(RecommendationFeedback.FeedbackTag.PRACTICAL, RecommendationFeedback.FeedbackTag.VARIED);
        assertThat(reloaded.get().getDislikedTags())
                .containsExactly(RecommendationFeedback.FeedbackTag.EXPENSIVE);
        assertThat(reloaded.get().getCreatedAt()).isNotNull();
        assertThat(reloaded.get().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("persists feedback with empty tag lists and null comment")
    void shouldPersistMinimalFeedback() {
        // given
        RecommendationFeedback feedback = new RecommendationFeedback();
        feedback.setRecommendation(recommendation);
        feedback.setRating(1);
        feedback.setComment(null);
        feedback.setLikedTags(List.of());
        feedback.setDislikedTags(List.of());

        // when
        feedbackRepository.save(feedback);
        entityManager.flush();
        entityManager.clear();

        // then
        RecommendationFeedback reloaded = feedbackRepository.findByRecommendationId(recommendation.getId()).orElseThrow();
        assertThat(reloaded.getRating()).isEqualTo(1);
        assertThat(reloaded.getComment()).isNull();
        assertThat(reloaded.getLikedTags()).isEmpty();
        assertThat(reloaded.getDislikedTags()).isEmpty();
    }

    @Test
    @DisplayName("deleteByRecommendationUserId removes feedback so user deletion is not blocked")
    void shouldDeleteFeedbackByUserId() {
        // given
        RecommendationFeedback feedback = new RecommendationFeedback();
        feedback.setRecommendation(recommendation);
        feedback.setRating(5);
        feedback.setLikedTags(List.of(RecommendationFeedback.FeedbackTag.TASTY));
        feedback.setDislikedTags(List.of());
        feedbackRepository.save(feedback);
        entityManager.flush();

        Integer userId = recommendation.getUser().getId();
        assertThat(feedbackRepository.existsByRecommendationId(recommendation.getId())).isTrue();

        // when
        feedbackRepository.deleteByRecommendationUserId(userId);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(feedbackRepository.existsByRecommendationId(recommendation.getId())).isFalse();
    }
}
