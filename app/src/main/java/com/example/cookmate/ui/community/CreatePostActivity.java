package com.example.cookmate.ui.community;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookmate.R;
import com.example.cookmate.network.ServerClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private TextView tvSelectedRecipe;
    private String selectedRecipeSteps = "";
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
        userId = sp.getLong("userId", 1);
        String username = sp.getString("username", "匿名");

        etTitle = findViewById(R.id.et_post_title);
        etContent = findViewById(R.id.et_post_content);
        tvSelectedRecipe = findViewById(R.id.tv_selected_recipe);
        Button btnPublish = findViewById(R.id.btn_publish);
        Button btnPickRecipe = findViewById(R.id.btn_pick_recipe);

        btnPickRecipe.setOnClickListener(v -> loadMyRecipes());

        btnPublish.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            if (title.isEmpty()) { Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show(); return; }

            ServerClient.addPost(userId, username, title, content, selectedRecipeSteps, new ServerClient.OnResultListener() {
                @Override public void onSuccess(String json) { runOnUiThread(() -> { Toast.makeText(CreatePostActivity.this, "发布成功", Toast.LENGTH_SHORT).show(); finish(); }); }
                @Override public void onError(String error) { runOnUiThread(() -> Toast.makeText(CreatePostActivity.this, "错误: " + error, Toast.LENGTH_LONG).show()); }
            });
        });
    }

    private void loadMyRecipes() {
        Toast.makeText(this, "userId: " + userId, Toast.LENGTH_SHORT).show();
        ServerClient.getRecipesByUser(userId, new ServerClient.OnResultListener() {
            @Override public void onSuccess(String json) {
                List<String> names = new ArrayList<>();
                List<String> stepsList = new ArrayList<>();
                try {
                    JSONArray data = new JSONObject(json).getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        names.add(obj.getString("name"));
                        stepsList.add(obj.optString("steps", ""));
                    }
                } catch (Exception e) { e.printStackTrace(); }

                if (names.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(CreatePostActivity.this, "你还没有生成过菜谱", Toast.LENGTH_SHORT).show());
                    return;
                }

                runOnUiThread(() -> {
                    new AlertDialog.Builder(CreatePostActivity.this)
                            .setTitle("选择菜谱")
                            .setItems(names.toArray(new String[0]), (dialog, which) -> {
                                selectedRecipeSteps = stepsList.get(which);
                                tvSelectedRecipe.setText("已选择：" + names.get(which));
                            })
                            .show();
                });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(CreatePostActivity.this, "加载失败", Toast.LENGTH_SHORT).show());
            }
        });
    }
}