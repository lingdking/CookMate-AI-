package com.example.cookmate.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "chat_history")
public class ChatHistory {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String sessionId;    // 会话ID，同一轮对话共享
    private String role;         // user 或 ai
    private String content;      // 消息内容
    private Date timestamp;      // 时间

    public ChatHistory() {
        this.timestamp = new Date();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}