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

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnRegister;
    private TextView tvGoLogin;
    private EditText etPasswordConfirm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.et_reg_username);
        etPassword = findViewById(R.id.et_reg_password);
        btnRegister = findViewById(R.id.btn_register);
        tvGoLogin = findViewById(R.id.tv_go_login);
        etPasswordConfirm = findViewById(R.id.et_reg_password_confirm);
        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String passwordConfirm = etPasswordConfirm.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请填写用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(passwordConfirm)) {
                Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }
            ServerClient.register(username, password, new ServerClient.OnResultListener() {
                @Override
                public void onSuccess(String json) {
                    SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
                    sp.edit().putString("username", username).putBoolean("isLogin", true).apply();

                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "注册失败: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        tvGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}