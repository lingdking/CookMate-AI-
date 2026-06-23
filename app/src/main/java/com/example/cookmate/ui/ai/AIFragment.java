package com.example.cookmate.ui.ai;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.R;
import com.example.cookmate.data.database.AppDatabase;
import com.example.cookmate.data.model.ChatHistory;
import com.example.cookmate.network.AIManager;
import com.example.cookmate.network.ServerClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AIFragment extends Fragment {

    private LinearLayout chatContainer;
    private ScrollView scrollChat;
    private EditText etMessage;
    private TextView btnSend, btnMenu, btnNewChat;
    private DrawerLayout drawerLayout;
    private RecyclerView rvSessions;
    private AppDatabase db;
    private String sessionId;
    private SharedPreferences sp;
    private List<String> sessions = new ArrayList<>();
    private SessionAdapter sessionAdapter;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai, container, false);

        chatContainer = view.findViewById(R.id.chat_container);
        scrollChat = view.findViewById(R.id.scroll_chat);
        etMessage = view.findViewById(R.id.et_message);
        btnSend = view.findViewById(R.id.btn_send);
        btnMenu = view.findViewById(R.id.btn_menu);
        btnNewChat = view.findViewById(R.id.btn_new_chat);
        drawerLayout = view.findViewById(R.id.drawer_layout);
        rvSessions = view.findViewById(R.id.rv_sessions);

        db = AppDatabase.getInstance(getContext());
        sp = requireContext().getSharedPreferences("ai_session", 0);
        sessionId = sp.getString("currentSession", UUID.randomUUID().toString());

        rvSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        sessionAdapter = new SessionAdapter();
        rvSessions.setAdapter(sessionAdapter);

        loadSessions();
        addAIBubble("你好！我是 CookMate AI 助手 👨‍🍳\n我可以帮你识别食材、生成菜谱、给出储存建议，随时问我吧！", new Date());

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.START));
        btnNewChat.setOnClickListener(v -> {
            sessionId = UUID.randomUUID().toString();
            sp.edit().putString("currentSession", sessionId).apply();
            chatContainer.removeAllViews();
            addAIBubble("你好！我是 CookMate AI 助手 👨‍🍳\n我可以帮你识别食材、生成菜谱、给出储存建议，随时问我吧！", new Date());
            drawerLayout.closeDrawers();
            loadSessions();
        });
        btnSend.setOnClickListener(v -> sendMessage());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sp.edit().putString("currentSession", sessionId).apply();
    }

    private void loadSessions() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<String> list = db.chatHistoryDao().getAllSessionsDirect();
            if (list != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    sessions.clear();
                    sessions.addAll(list);
                    sessionAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        etMessage.setText("");

        if (text.contains("食材") || text.contains("菜谱") || text.contains("做什么") || text.contains("能做什么") || text.contains("推荐")) {
            addUserBubble(text, new Date());
            addAIBubble("正在读取你的食材库...", new Date());

            long userId = requireContext().getSharedPreferences("user", 0).getLong("userId", 1);
            ServerClient.getFoodItems(userId, new ServerClient.OnResultListener() {
                @Override
                public void onSuccess(String json) {
                    List<String> names = new ArrayList<>();
                    try {
                        JSONArray data = new JSONObject(json).getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            names.add(data.getJSONObject(i).getString("name"));
                        }
                    } catch (Exception e) { e.printStackTrace(); }

                    String ingredients = names.isEmpty() ? "暂无食材" : String.join("、", names);

                    ChatHistory userMsg = new ChatHistory();
                    userMsg.setSessionId(sessionId);
                    userMsg.setRole("user");
                    userMsg.setContent(text);
                    AppDatabase.databaseWriteExecutor.execute(() -> db.chatHistoryDao().insert(userMsg));

                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        removeLastMessage();
                        addAIBubble("思考中...", new Date());
                        String systemPrompt = "用户当前食材库：" + ingredients + "。请根据这些食材回答用户问题。";
                        AIManager.getInstance().chat(text, systemPrompt, new AIManager.AICallback() {
                            @Override
                            public void onSuccess(String result) {
                                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                    removeLastMessage();
                                    addAIBubble(result, new Date());
                                    ChatHistory aiMsg = new ChatHistory();
                                    aiMsg.setSessionId(sessionId);
                                    aiMsg.setRole("ai");
                                    aiMsg.setContent(result);
                                    AppDatabase.databaseWriteExecutor.execute(() -> db.chatHistoryDao().insert(aiMsg));
                                });
                            }
                            @Override
                            public void onError(String error) {
                                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                    removeLastMessage();
                                    Toast.makeText(getActivity(), "AI错误: " + error, Toast.LENGTH_LONG).show();
                                    addAIBubble("抱歉，出错了😢", new Date());
                                });
                            }
                        });
                    });
                }
                @Override
                public void onError(String error) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        removeLastMessage();
                        addAIBubble("读取食材失败: " + error, new Date());
                    });
                }
            });
        } else {
            addUserBubble(text, new Date());
            ChatHistory userMsg = new ChatHistory();
            userMsg.setSessionId(sessionId);
            userMsg.setRole("user");
            userMsg.setContent(text);
            AppDatabase.databaseWriteExecutor.execute(() -> db.chatHistoryDao().insert(userMsg));

            addAIBubble("思考中...", new Date());
            AIManager.getInstance().chat(text, new AIManager.AICallback() {
                @Override
                public void onSuccess(String result) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        removeLastMessage();
                        addAIBubble(result, new Date());
                        ChatHistory aiMsg = new ChatHistory();
                        aiMsg.setSessionId(sessionId);
                        aiMsg.setRole("ai");
                        aiMsg.setContent(result);
                        AppDatabase.databaseWriteExecutor.execute(() -> db.chatHistoryDao().insert(aiMsg));
                    });
                }
                @Override
                public void onError(String error) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        removeLastMessage();
                        Toast.makeText(getActivity(), "AI错误: " + error, Toast.LENGTH_LONG).show();
                        addAIBubble("抱歉，出错了😢", new Date());
                    });
                }
            });
        }
    }

    private void addUserBubble(String text, Date time) {
        LinearLayout wrapper = new LinearLayout(getContext());
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setGravity(Gravity.END);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.setMargins(0, 6, 0, 6);
        wrapper.setLayoutParams(wp);
        TextView tvTime = new TextView(getContext());
        tvTime.setText(timeFormat.format(time));
        tvTime.setTextSize(10);
        tvTime.setTextColor(0xFF999999);
        tvTime.setPadding(0, 0, 8, 0);
        wrapper.addView(tvTime);
        TextView tv = new TextView(getContext());
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
        LinearLayout wrapper = new LinearLayout(getContext());
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.setGravity(Gravity.START);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wp.setMargins(0, 6, 0, 6);
        wrapper.setLayoutParams(wp);
        TextView avatar = new TextView(getContext());
        avatar.setText("🤖");
        avatar.setTextSize(24);
        avatar.setGravity(Gravity.CENTER);
        avatar.setPadding(0, 0, 8, 0);
        wrapper.addView(avatar);
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(16, 10, 16, 10);
        tv.setTextSize(14);
        tv.setTextColor(0xFF1A1A1A);
        tv.setBackgroundResource(R.drawable.bubble_ai);
        wrapper.addView(tv);
        TextView tvTime = new TextView(getContext());
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

    private class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.VH> {
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session, parent, false));
        }
        @Override
        public void onBindViewHolder(VH holder, int pos) {
            String sid = sessions.get(pos);
            holder.tvId.setText("对话 " + (pos + 1));
            holder.tvPreview.setText("");
            holder.itemView.setOnClickListener(v -> {
                sessionId = sid;
                loadHistory();
                drawerLayout.closeDrawers();
            });
            holder.btnDelete.setOnClickListener(v -> {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    db.chatHistoryDao().deleteSession(sid);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            sessions.remove(pos);
                            notifyItemRemoved(pos);
                            if (sid.equals(sessionId)) {
                                sessionId = UUID.randomUUID().toString();
                                sp.edit().putString("currentSession", sessionId).apply();
                                chatContainer.removeAllViews();
                                addAIBubble("你好！我是 CookMate AI 助手 👨‍🍳\n我可以帮你识别食材、生成菜谱、给出储存建议，随时问我吧！", new Date());
                            }
                        });
                    }
                });
            });
        }
        @Override
        public int getItemCount() { return sessions.size(); }
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

    private void loadHistory() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ChatHistory> list = db.chatHistoryDao().getMessagesBySessionDirect(sessionId);
            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                chatContainer.removeAllViews();
                if (list != null) {
                    for (ChatHistory msg : list) {
                        if ("user".equals(msg.getRole())) addUserBubble(msg.getContent(), msg.getTimestamp());
                        else addAIBubble(msg.getContent(), msg.getTimestamp());
                    }
                }
            });
        });
    }
}