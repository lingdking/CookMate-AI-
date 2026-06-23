package com.example.cookmate.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.cookmate.data.dao.RecipeDao;
import com.example.cookmate.data.database.AppDatabase;
import com.example.cookmate.data.model.Recipe;

import java.util.List;

public class RecipeRepository {

    private final RecipeDao recipeDao;
    private final LiveData<List<Recipe>> allRecipes;

    public RecipeRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        recipeDao = db.recipeDao();
        allRecipes = recipeDao.getAllRecipes();
    }

    public LiveData<List<Recipe>> getAllRecipes() {
        return allRecipes;
    }

    public LiveData<List<Recipe>> getFavoriteRecipes() {
        return recipeDao.getFavoriteRecipes();
    }

    public LiveData<List<Recipe>> getRecipesByType(String type) {
        return recipeDao.getRecipesByType(type);
    }

    public Recipe getRecipeById(long id) {
        return recipeDao.getRecipeById(id);
    }

    public long insertRecipe(Recipe recipe) {
        final long[] result = new long[1];
        try {
            AppDatabase.databaseWriteExecutor.submit(() -> {
                result[0] = recipeDao.insertRecipe(recipe);
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result[0];
    }

    public void updateRecipe(Recipe recipe) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            recipeDao.updateRecipe(recipe);
        });
    }

    public void toggleFavorite(long id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            recipeDao.toggleFavorite(id);
        });
    }

    public void markAsCooked(long id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            recipeDao.markAsCooked(id);
        });
    }

    public void deleteRecipe(Recipe recipe) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            recipeDao.deleteRecipe(recipe);
        });
    }
}