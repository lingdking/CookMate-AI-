package com.example.cookmate.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cookmate.R;
import com.example.cookmate.network.ServerClient;
import com.example.cookmate.ui.user.LoginActivity;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView tvUsername = view.findViewById(R.id.tv_username);
        Button btnLogout = view.findViewById(R.id.btn_logout);
        Button btnDeleteAccount = view.findViewById(R.id.btn_delete_account);

        SharedPreferences sp = requireContext().getSharedPreferences("user", 0);
        String username = sp.getString("username", "未登录");
        tvUsername.setText(username);

        btnLogout.setOnClickListener(v -> {
            sp.edit().clear().apply();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        btnDeleteAccount.setOnClickListener(v -> showDeleteDialog(username, sp));

        return view;
    }

    private void showDeleteDialog(String username, SharedPreferences sp) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_delete_account, null);
        EditText etUsername = dialogView.findViewById(R.id.et_del_username);
        EditText etPassword = dialogView.findViewById(R.id.et_del_password);

        new AlertDialog.Builder(getContext())
                .setTitle("注销账号")
                .setMessage("输入账号密码确认注销，此操作不可撤销")
                .setView(dialogView)
                .setPositiveButton("确认注销", (dialog, which) -> {
                    String inputUsername = etUsername.getText().toString().trim();
                    String inputPassword = etPassword.getText().toString().trim();

                    if (!username.equals(inputUsername)) {
                        Toast.makeText(getContext(), "用户名不匹配", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ServerClient.login(inputUsername, inputPassword, new ServerClient.OnResultListener() {
                        @Override
                        public void onSuccess(String json) {
                            long userId = sp.getLong("userId", -1);
                            ServerClient.deleteUser(userId, new ServerClient.OnResultListener() {
                                @Override
                                public void onSuccess(String json2) {
                                    sp.edit().clear().apply();
                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                }
                                @Override
                                public void onError(String error) {
                                    Toast.makeText(getContext(), "注销失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), "密码错误", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }
}