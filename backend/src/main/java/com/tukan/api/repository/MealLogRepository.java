package com.tukan.api.repository;

import com.tukan.api.entity.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    List<MealLog> findByUserIdAndMealDateBetween(Integer userId, LocalDate start, LocalDate end);

    List<MealLog> findByUserIdAndMealDate(Integer userId, LocalDate date);

    boolean existsByUserIdAndMealDateAndMealType(Integer userId, LocalDate date, String mealType);

    Optional<MealLog> findByIdAndUserId(Long id, Integer userId);

    @Modifying(clearAutomatically = true)
    @Query("delete from MealLog m where m.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}
