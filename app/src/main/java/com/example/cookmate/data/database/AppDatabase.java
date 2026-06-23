package com.example.cookmate.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.cookmate.data.dao.ChatHistoryDao;
import com.example.cookmate.data.dao.CommunityPostDao;
import com.example.cookmate.data.dao.FoodItemDao;
import com.example.cookmate.data.dao.RecipeDao;
import com.example.cookmate.data.model.ChatHistory;
import com.example.cookmate.data.model.CommunityPost;
import com.example.cookmate.data.model.FoodItem;
import com.example.cookmate.data.model.Recipe;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CookMate 应用的 Room 数据库主类
 * 
 * 这是整个应用数据库的核心入口点，负责：
 * 1. 定义数据库结构（包含的表和版本）
 * 2. 提供 DAO（数据访问对象）的获取方法
 * 3. 管理数据库实例的单例模式
 * 4. 配置数据库构建选项
 * 
 * 数据库名称：cookmate_database
 * 数据库版本：2
 * 
 * 包含的数据表：
 * - food_items: 食材库存表
 * - recipes: 菜谱表
 * - community_posts: 社区帖子表
 * - chat_history: AI聊天记录表
 * 
 * 使用示例：
 * <pre>
 * AppDatabase db = AppDatabase.getInstance(context);
 * RecipeDao recipeDao = db.recipeDao();
 * List<Recipe> recipes = recipeDao.getAllRecipes();
 * </pre>
 */
@Database(
        // 声明数据库中包含的所有实体（表）
        // 每个 class 对应数据库中的一张表
        entities = {FoodItem.class, Recipe.class, CommunityPost.class, ChatHistory.class},
        // 数据库版本号，修改表结构时需要递增
        version = 2,
        // 不导出 Schema 文件到磁盘
        // 设为 true 可用于版本控制和迁移测试
        exportSchema = false
)
// 注册类型转换器，将 Date 等非 SQLite 原生类型转换为支持的类型
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    /**
     * 获取食材数据访问对象
     * @return FoodItemDao 食材操作接口
     */
    public abstract FoodItemDao foodItemDao();
    
    /**
     * 获取菜谱数据访问对象
     * @return RecipeDao 菜谱操作接口
     */
    public abstract RecipeDao recipeDao();
    
    /**
     * 获取社区帖子数据访问对象
     * @return CommunityPostDao 帖子操作接口
     */
    public abstract CommunityPostDao communityPostDao();
    
    /**
     * 获取聊天记录数据访问对象
     * @return ChatHistoryDao 聊天记录操作接口
     */
    public abstract ChatHistoryDao chatHistoryDao();

    // 单例实例，使用 volatile 保证多线程可见性
    private static volatile AppDatabase INSTANCE;
    
    /**
     * 数据库写入线程池
     * 
     * 创建一个固定大小为 4 的线程池，用于在后台线程执行数据库写操作。
     * Room 要求所有数据库操作必须在子线程执行，避免阻塞主线程导致 ANR。
     * 
     * 使用示例：
     * <pre>
     * AppDatabase.databaseWriteExecutor.execute(() -> {
     *     db.recipeDao().insertRecipe(recipe);
     * });
     * </pre>
     */
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    /**
     * 获取数据库单例实例（双重检查锁定模式）
     * 
     * 使用单例模式确保整个应用中只有一个数据库实例，避免：
     * - 资源浪费（多次打开数据库消耗内存和文件句柄）
     * - 数据不一致（多个实例可能导致并发问题）
     * 
     * 实现细节：
     * 1. 第一次检查：如果 INSTANCE 不为 null，直接返回（避免不必要的同步）
     * 2. 同步锁：确保多线程环境下只创建一个实例
     * 3. 第二次检查：防止多个线程同时通过第一次检查后重复创建
     * 4. 使用 context.getApplicationContext() 避免内存泄漏
     * 
     * 性能优化：
     * - 双重检查减少同步开销
     * - volatile 禁止指令重排序，确保安全发布
     * 
     * @param context 上下文对象（会使用 ApplicationContext，避免内存泄漏）
     * @return AppDatabase 数据库单例实例
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            // 同步块，确保线程安全
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // 首次创建数据库实例
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),  // 使用应用上下文，避免 Activity 泄漏
                                    AppDatabase.class,                 // 数据库类
                                    "cookmate_database"                // 数据库文件名
                            )
                            // 破坏性迁移策略：
                            // 当数据库版本升级时，删除旧表并重新创建
                            // ⚠️ 注意：这会丢失所有数据！
                            // 生产环境应该使用 .addMigration() 实现无损迁移
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}