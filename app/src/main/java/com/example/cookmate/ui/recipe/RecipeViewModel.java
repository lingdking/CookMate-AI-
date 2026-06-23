package com.example.cookmate.ui.recipe;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.cookmate.data.model.Recipe;
import com.example.cookmate.data.repository.RecipeRepository;

import java.util.List;

public class RecipeViewModel extends AndroidViewModel {

    private final RecipeRepository repository;
    private final LiveData<List<Recipe>> allRecipes;

    public RecipeViewModel(Application application) {
        super(application);
        repository = new RecipeRepository(application);
        allRecipes = repository.getAllRecipes();
    }

    public LiveData<List<Recipe>> getAllRecipes() {
        return allRecipes;
    }
}