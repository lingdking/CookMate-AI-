package com.example.cookmate.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.cookmate.data.model.ChatHistory;
import java.util.List;

/**
 * 聊天记录数据访问对象（DAO）
 * 
 * 定义了对 chat_history 表的所有数据库操作。
 * Room 框架会在编译时自动生成实现类（ChatHistoryDao_Impl）。
 * 
 * 主要功能：
 * - 管理 AI 聊天会话的历史记录
 * - 支持按会话ID查询消息
 * - 提供 LiveData 响应式查询和同步查询两种方式
 * 
 * 数据库表结构：
 * - id: 主键，自增
 * - sessionId: 会话ID，同一轮对话共享
 * - role: 角色（user/ai）
 * - content: 消息内容
 * - timestamp: 时间戳
 */
@Dao
public interface ChatHistoryDao {

    /**
     * 获取所有不重复的会话ID列表（响应式）
     * 
     * SQL说明：
     * - DISTINCT: 去重，每个会话只显示一次
     * - ORDER BY timestamp DESC: 按最新消息时间倒序排列
     * 
     * 使用场景：显示历史会话列表
     * 
     * @return LiveData<List<String>> 会话ID列表的响应式观察者
     */
    @Query("SELECT DISTINCT sessionId FROM chat_history ORDER BY timestamp DESC")
    LiveData<List<String>> getAllSessions();

    /**
     * 获取指定会话的所有消息（响应式）
     * 
     * SQL说明：
     * - WHERE sessionId = :sessionId: 过滤指定会话
     * - ORDER BY timestamp ASC: 按时间正序排列（旧消息在前，新消息在后）
     * 
     * 使用场景：加载某个会话的完整聊天记录
     * 
     * @param sessionId 会话唯一标识
     * @return LiveData<List<ChatHistory>> 消息列表的响应式观察者
     */
    @Query("SELECT * FROM chat_history WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    LiveData<List<ChatHistory>> getMessagesBySession(String sessionId);

    /**
     * 获取指定会话的最后一条消息（响应式）
     * 
     * SQL说明：
     * - ORDER BY timestamp DESC: 按时间倒序
     * - LIMIT 1: 只取第一条（即最新消息）
     * 
     * 使用场景：在会话列表中显示最后一条消息作为预览
     * 
     * @param sessionId 会话唯一标识
     * @return LiveData<ChatHistory> 单条消息的响应式观察者（可能为null）
     */
    @Query("SELECT * FROM chat_history WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    LiveData<ChatHistory> getLastMessage(String sessionId);

    /**
     * 插入单条聊天记录
     * 
     * Room 自动生成 INSERT 语句，处理所有字段的绑定。
     * 
     * 使用场景：保存用户发送的消息或AI回复
     * 
     * @param chatHistory 要插入的聊天记录对象
     */
    @Insert
    void insert(ChatHistory chatHistory);

    /**
     * 删除指定会话的所有聊天记录
     * 
     * SQL说明：
     * - DELETE FROM chat_history WHERE sessionId = :sessionId
     * 
     * 使用场景：用户删除某个历史会话
     * 
     * @param sessionId 要删除的会话ID
     */
    @Query("DELETE FROM chat_history WHERE sessionId = :sessionId")
    void deleteSession(String sessionId);
    
    /**
     * 获取所有不重复的会话ID列表（同步版本）
     * 
     * 与 getAllSessions() 的区别：
     * - 立即返回结果，不是响应式的
     * - 必须在子线程调用，避免阻塞主线程
     * 
     * 使用场景：AIFragment.loadSessions() 中需要即时数据
     * 
     * @return List<String> 会话ID列表
     */
    @Query("SELECT DISTINCT sessionId FROM chat_history ORDER BY timestamp DESC")
    List<String> getAllSessionsDirect();

    /**
     * 获取指定会话的所有消息（同步版本）
     * 
     * 与 getMessagesBySession() 的区别：
     * - 立即返回结果，不是响应式的
     * - 必须在子线程调用
     * 
     * 使用场景：AIFragment.loadHistory() 加载历史消息
     * 
     * @param sessionId 会话唯一标识
     * @return List<ChatHistory> 消息列表（按时间正序）
     */
    @Query("SELECT * FROM chat_history WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<ChatHistory> getMessagesBySessionDirect(String sessionId);
}