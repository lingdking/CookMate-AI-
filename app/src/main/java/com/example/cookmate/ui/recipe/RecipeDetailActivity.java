package com.example.cookmate.ui.recipe;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookmate.R;

public class RecipeDetailActivity extends AppCompatActivity {

    private TextView tvName, tvType, tvCookingTime, tvIngredients, tvSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tvName = findViewById(R.id.tv_recipe_name);
        tvType = findViewById(R.id.tv_recipe_type);
        tvCookingTime = findViewById(R.id.tv_cooking_time);
        tvIngredients = findViewById(R.id.tv_ingredients);
        tvSteps = findViewById(R.id.tv_steps);

        String name = getIntent().getStringExtra("recipe_name");
        String steps = getIntent().getStringExtra("recipe_steps");

        tvName.setText(name != null ? name : "");
        tvType.setText("AI智能生成");
        tvSteps.setText(steps != null ? steps : "");

        if (steps != null) {
            for (String line : steps.split("\n")) {
                line = line.trim();
                if (line.startsWith("所用食材：") || line.startsWith("所用食材:")) {
                    tvIngredients.setText("🥬 " + line.replace("所用食材：", "").replace("所用食材:", "").trim());
                }
                if (line.contains("分钟") && (line.startsWith("烹饪时间") || line.length() < 15)) {
                    tvCookingTime.setText("⏱ " + line.replace("烹饪时间：", "").replace("烹饪时间:", "").trim());
                }
            }
        }
    }
}