package com.tukan.api.repository;

import com.tukan.api.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Integer> {

    @Query("SELECT f FROM Food f WHERE f.active = true AND f.suitableMeals LIKE %:mealType%")
    List<Food> findActiveBySuitableMeal(@Param("mealType") String mealType);

    List<Food> findByActiveTrue();
}
