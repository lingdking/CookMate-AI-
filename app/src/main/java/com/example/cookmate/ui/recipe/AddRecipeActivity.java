package com.example.cookmate.ui.recipe;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookmate.R;
import com.example.cookmate.network.ServerClient;

public class AddRecipeActivity extends AppCompatActivity {

    private EditText etName, etIngredients, etCookingTime, etSteps;
    private Spinner spinnerDifficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etName = findViewById(R.id.et_recipe_name);
        etIngredients = findViewById(R.id.et_ingredients);
        etCookingTime = findViewById(R.id.et_cooking_time);
        etSteps = findViewById(R.id.et_steps);
        spinnerDifficulty = findViewById(R.id.spinner_difficulty);
        Button btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> saveRecipe());
    }

    private void saveRecipe() {
        String name = etName.getText().toString().trim();
        String ingredients = etIngredients.getText().toString().trim();
        String timeStr = etCookingTime.getText().toString().trim();
        String steps = etSteps.getText().toString().trim();
        String difficulty = spinnerDifficulty.getSelectedItem().toString();

        if (name.isEmpty() || ingredients.isEmpty() || steps.isEmpty()) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }

        int cookingTime = 30;
        try { cookingTime = Integer.parseInt(timeStr); } catch (Exception e) {}

        long userId = getSharedPreferences("user", MODE_PRIVATE).getLong("userId", 1);

        ServerClient.addRecipe(name, ingredients, steps, cookingTime, difficulty, userId,
                new ServerClient.OnResultListener() {
                    @Override public void onSuccess(String json) {
                        runOnUiThread(() -> {
                            Toast.makeText(AddRecipeActivity.this, "菜谱已发布！", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                    @Override public void onError(String error) {
                        runOnUiThread(() -> Toast.makeText(AddRecipeActivity.this, "发布失败: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
    }
}