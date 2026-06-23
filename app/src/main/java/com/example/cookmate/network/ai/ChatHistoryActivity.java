package com.example.cookmate.network.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.R;
import com.example.cookmate.data.database.AppDatabase;
import com.example.cookmate.data.model.ChatHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatHistoryActivity extends AppCompatActivity {

    private RecyclerView rvSessions;
    private TextView btnBack, btnNewChat;
    private AppDatabase db;
    private SessionAdapter adapter;
    private List<String> sessions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history);

        rvSessions = findViewById(R.id.rv_sessions);
        btnBack = findViewById(R.id.btn_back_history);
        btnNewChat = findViewById(R.id.btn_new_chat);
        db = AppDatabase.getInstance(this);

        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionAdapter();
        rvSessions.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnNewChat.setOnClickListener(v -> {
            openChat("");
        });

        loadSessions();
    }

    private void loadSessions() {
        db.chatHistoryDao().getAllSessions().observe(this, sessionList -> {
            if (sessionList != null) {
                sessions = sessionList;
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void openChat(String sessionId) {
        Intent intent = new Intent(this, AIChatActivity.class);
        intent.putExtra("session_id", sessionId);
        startActivity(intent);
        finish();
    }

    private class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {

        private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_session, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String sessionId = sessions.get(position);
            holder.tvSessionId.setText("对话 #" + sessionId.substring(Math.max(0, sessionId.length() - 6)));

            db.chatHistoryDao().getLastMessage(sessionId).observe(ChatHistoryActivity.this, msg -> {
                if (msg != null) {
                    holder.tvPreview.setText(msg.getContent());
                    holder.tvTime.setText(sdf.format(msg.getTimestamp()));
                }
            });

            holder.itemView.setOnClickListener(v -> openChat(sessionId));
        }

        @Override
        public int getItemCount() {
            return sessions.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSessionId, tvPreview, tvTime;

            ViewHolder(View v) {
                super(v);
                tvSessionId = v.findViewById(R.id.tv_session_id);
                tvPreview = v.findViewById(R.id.tv_session_preview);
                tvTime = v.findViewById(R.id.tv_session_time);
            }
        }
    }
}