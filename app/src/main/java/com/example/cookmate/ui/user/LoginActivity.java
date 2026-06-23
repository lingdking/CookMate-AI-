package com.example.cookmate.ui.user;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookmate.MainActivity;
import com.example.cookmate.R;
import com.example.cookmate.network.ServerClient;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvGoRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoRegister = findViewById(R.id.tv_go_register);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请填写用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }

            ServerClient.login(username, password, new ServerClient.OnResultListener() {
                @Override
                public void onSuccess(String json) {
                    try {
                        JSONObject root = new JSONObject(json);
                        JSONObject data = root.getJSONObject("data");
                        long userId = data.getLong("userId");

                        SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
                        sp.edit()
                                .putString("username", username)
                                .putLong("userId", userId)
                                .putBoolean("isLogin", true)
                                .apply();

                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "账号或密码错误", Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "错误: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        tvGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
}