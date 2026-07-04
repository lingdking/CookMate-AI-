package com.cookmate.controller;

import com.cookmate.entity.Recipe;
import com.cookmate.repository.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RecipeController {

    @Autowired
    private RecipeRepository recipeRepository;

    @GetMapping("/recipes")
    public Map<String, Object> getRecipes() {
        List<Recipe> recipes = recipeRepository.findAllByOrderByCreatedAtDesc();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", recipes);
        return result;
    }

    @PostMapping("/recipes")
    public Map<String, Object> addRecipe(@RequestBody Recipe recipe) {
        Recipe saved = recipeRepository.save(recipe);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "发布成功");
        result.put("data", Map.of("id", saved.getId(), "name", saved.getName()));
        return result;
    }
    @DeleteMapping("/recipes/{id}")
    public Map<String, Object> deleteRecipe(@PathVariable Long id) {
        recipeRepository.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }
    @GetMapping("/recipes/user/{userId}")
    public Map<String, Object> getRecipesByUser(@PathVariable Long userId) {
        List<Recipe> recipes = recipeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", recipes);
        return result;
    }
}