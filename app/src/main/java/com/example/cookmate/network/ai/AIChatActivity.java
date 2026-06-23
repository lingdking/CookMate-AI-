package com.example.cookmate.network.ai;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.R;
import com.example.cookmate.data.database.AppDatabase;
import com.example.cookmate.data.model.ChatHistory;
import com.example.cookmate.network.AIManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AIChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private ScrollView scrollChat;
    private EditText etMessage;
    private TextView btnSend, btnClose, btnMenu, btnNewChat;
    private DrawerLayout drawerLayout;
    private RecyclerView rvSessions;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private AppDatabase db;
    private String sessionId;
    private List<String> sessions = new ArrayList<>();
    private SessionAdapter sessionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        btnClose = findViewById(R.id.btn_close);
        setContentView(R.layout.activity_ai_chat);

        chatContainer = findViewById(R.id.chat_container);
        scrollChat = findViewById(R.id.scroll_chat);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnClose = findViewById(R.id.btn_close);
        btnMenu = findViewById(R.id.btn_menu);
        btnNewChat = findViewById(R.id.btn_new_chat);
        drawerLayout = findViewById(R.id.drawer_layout);
        rvSessions = findViewById(R.id.rv_sessions);

        db = AppDatabase.getInstance(this);

        sessionId = getIntent().getStringExtra("session_id");
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        // 侧边栏列表
        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        sessionAdapter = new SessionAdapter();
        rvSessions.setAdapter(sessionAdapter);
        loadSessions();

        // 加载当前会话历史
        loadHistory();

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));
        btnClose.setOnClickListener(v -> finish());
        btnNewChat.setOnClickListener(v -> {
            sessionId = UUID.randomUUID().toString();
            chatContainer.removeAllViews();
            addAIBubble("你好！我是 CookMate AI 助手 👨‍🍳\n我可以帮你识别食材、生成菜谱、给出储存建议，随时问我吧！", new Date());
            drawerLayout.closeDrawers();
        });

        btnSend.setOnClickListener(v -> sendMessage());
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND
                    || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                    && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void loadSessions() {
        db.chatHistoryDao().getAllSessions().observe(this, list -> {
            if (list != null) {
                sessions = list;
                sessionAdapter.notifyDataSetChanged();
            }
        });
    }

    private void loadHistory() {
        db.chatHistoryDao().getMessagesBySession(sessionId).observe(this, messages -> {
            chatContainer.removeAllViews();
            if (messages == null || messages.isEmpty()) {
                addAIBubble("你好！我是 CookMate AI 助手 👨‍🍳\n我可以帮你识别食材、生成菜谱、给出储存建议，随时问我吧！", new Date());
            } else {
                for (ChatHistory msg : messages) {
                    if ("user".equals(msg.getRole())) {
                        addUserBubble(msg.getContent(), msg.getTimestamp());
                    } else {
                        addAIBubble(msg.getContent(), msg.getTimestamp());
                    }
                }
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        etMessage.setText("");

        ChatHistory userMsg = new ChatHistory();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(text);
        AppDatabase.databaseWriteExecutor.execute(() -> db.chatHistoryDao().insert(userMsg));

        addAIBubble("思考中...", new Date());

        AIManager.getInstance().chat(text, new AIManager.AICallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    removeLastMessage();
                    ChatHistory aiMsg = new ChatHistory();
                    aiMsg.setSessionId(sessionId);
                    aiMsg.setRole("ai");
                    aiMsg.setContent(result);
                    AppDatabase.databaseWriteExecutor.execute(() -> db.chatHistoryDao().insert(aiMsg));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    removeLastMessage();
                    ChatHistory errMsg = new ChatHistory();
                    errMsg.setSessionId(sessionId);
                    errMsg.setRole("ai");
                    errMsg.setContent("抱歉，出错了😢\n" + error);
                    AppDatabase.databaseWriteExecutor.execute(() -> db.chatHistoryDao().insert(errMsg));
                });
            }
        });
    }

    private void addUserBubble(String text, Date time) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setGravity(Gravity.END);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.setMargins(0, 6, 0, 6);
        wrapper.setLayoutParams(wp);

        TextView tvTime = new TextView(this);
        tvTime.setText(timeFormat.format(time));
        tvTime.setTextSize(10);
        tvTime.setTextColor(0xFF999999);
        tvTime.setPadding(0, 0, 8, 0);
        wrapper.addView(tvTime);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(16, 10, 16, 10);
        tv.setTextSize(14);
        tv.setTextColor(0xFFFFFFFF);
        tv.setBackgroundResource(R.drawable.bubble_user);
        wrapper.addView(tv);

        chatContainer.addView(wrapper);
        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }

    private void addAIBubble(String text, Date time) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setGravity(Gravity.START);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.setMargins(0, 6, 0, 6);
        wrapper.setLayoutParams(wp);

        TextView avatar = new TextView(this);
        avatar.setText("🤖");
        avatar.setTextSize(24);
        avatar.setGravity(Gravity.CENTER);
        avatar.setPadding(0, 0, 8, 0);
        wrapper.addView(avatar);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(16, 10, 16, 10);
        tv.setTextSize(14);
        tv.setTextColor(0xFF1A1A1A);
        tv.setBackgroundResource(R.drawable.bubble_ai);
        wrapper.addView(tv);

        TextView tvTime = new TextView(this);
        tvTime.setText(timeFormat.format(time));
        tvTime.setTextSize(10);
        tvTime.setTextColor(0xFF999999);
        tvTime.setPadding(8, 0, 0, 0);
        wrapper.addView(tvTime);

        chatContainer.addView(wrapper);
        scrollChat.post(() -> scrollChat.fullScroll(View.FOCUS_DOWN));
    }

    private void removeLastMessage() {
        int count = chatContainer.getChildCount();
        if (count > 0) chatContainer.removeViewAt(count - 1);
    }

    // 侧边栏适配器
    // 侧边栏适配器
    private class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {

        private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            String sid = sessions.get(position);
            holder.tvId.setText("对话 " + (position + 1));

            db.chatHistoryDao().getLastMessage(sid).observe(AIChatActivity.this, msg -> {
                if (msg != null) {
                    holder.tvPreview.setText(msg.getContent().length() > 20
                            ? msg.getContent().substring(0, 20) + "..."
                            : msg.getContent());
                    holder.tvTime.setText(sdf.format(msg.getTimestamp()));
                }
            });

            holder.itemView.setOnClickListener(v -> {
                sessionId = sid;
                loadHistory();
                drawerLayout.closeDrawers();
            });

            holder.btnDelete.setOnClickListener(v -> {
                AppDatabase.databaseWriteExecutor.execute(() -> db.chatHistoryDao().deleteSession(sid));
            });
        }

        @Override
        public int getItemCount() {
            return sessions.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvId, tvPreview, tvTime, btnDelete;
            VH(View v) {
                super(v);
                tvId = v.findViewById(R.id.tv_session_id);
                tvPreview = v.findViewById(R.id.tv_session_preview);
                tvTime = v.findViewById(R.id.tv_session_time);
                btnDelete = v.findViewById(R.id.btn_delete_session);
            }
        }
    }
}