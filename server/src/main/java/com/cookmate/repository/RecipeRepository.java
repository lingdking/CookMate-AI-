package com.cookmate.repository;

import com.cookmate.entity.FoodItem;
import com.cookmate.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findAllByOrderByCreatedAtDesc();
    List<Recipe> findByUserIdOrderByCreatedAtDesc(Long userId);
}