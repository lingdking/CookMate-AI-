package com.example.cookmate.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cookmate.data.model.CommunityPost;

import java.util.List;

/**
 * 社区帖子数据访问对象（DAO）
 * 
 * 定义了对 community_posts 表的所有数据库操作。
 * Room 框架会在编译时自动生成实现类（CommunityPostDao_Impl）。
 * 
 * 主要功能：
 * - 管理社区帖子的增删改查
 * - 支持按类型筛选帖子（赠送/交换/求助）
 * - 提供帖子浏览次数统计和关闭功能
 * - 所有查询默认只返回"有效"状态的帖子
 * 
 * 数据库表结构：
 * - id: 主键，自增
 * - postType: 帖子类型（赠送/交换/求助）
 * - foodItemId: 关联的食材ID（可空）
 * - title: 标题
 * - content: 内容描述
 * - imagePaths: 图片路径（逗号分隔）
 * - userId: 发布者用户ID
 * - userName: 发布者昵称
 * - longitude: 经度
 * - latitude: 纬度
 * - locationDescription: 位置描述
 * - status: 状态（有效/已处理/已关闭）
 * - viewCount: 浏览次数
 * - adoptCount: 被采纳次数
 * - createdAt: 创建时间
 * - expiryTime: 过期时间
 */
@Dao
public interface CommunityPostDao {

    /**
     * 获取所有有效状态的帖子（响应式）
     * 
     * SQL说明：
     * - WHERE status = '有效': 只查询有效状态的帖子
     * - ORDER BY createdAt DESC: 按创建时间倒序，最新帖子在前
     * 
     * 使用场景：社区页面显示所有有效帖子列表
     * 
     * @return LiveData<List<CommunityPost>> 有效帖子列表的响应式观察者
     */
    @Query("SELECT * FROM community_posts WHERE status = '有效' ORDER BY createdAt DESC")
    LiveData<List<CommunityPost>> getAllActivePosts();

    /**
     * 根据帖子类型获取帖子（响应式）
     * 
     * SQL说明：
     * - WHERE postType = :type: 过滤指定类型的帖子
     * - AND status = '有效': 同时要求状态为有效
     * - ORDER BY createdAt DESC: 按创建时间倒序
     * 
     * 使用场景：筛选特定类型的帖子（如只看"赠送"或"交换"帖子）
     * 
     * @param type 帖子类型（赠送/交换/求助）
     * @return LiveData<List<CommunityPost>> 指定类型帖子列表的响应式观察者
     */
    @Query("SELECT * FROM community_posts WHERE postType = :type AND status = '有效' ORDER BY createdAt DESC")
    LiveData<List<CommunityPost>> getPostsByType(String type);

    /**
     * 根据ID获取单个帖子
     * 
     * 注意：这是同步方法，返回单个对象而非LiveData。
     * 需要在子线程调用。
     * 
     * 使用场景：查看帖子详情
     * 
     * @param id 帖子ID
     * @return CommunityPost 帖子对象（如果不存在则返回null）
     */
    @Query("SELECT * FROM community_posts WHERE id = :id")
    CommunityPost getPostById(long id);

    /**
     * 插入社区帖子
     * 
     * 冲突策略：OnConflictStrategy.REPLACE
     * - 如果主键或唯一约束冲突，则替换已有记录
     * - 返回插入的行ID
     * 
     * 使用场景：发布新帖子
     * 
     * @param post 要插入的帖子对象
     * @return long 插入的帖子ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPost(CommunityPost post);

    /**
     * 更新帖子信息
     * 
     * Room 根据主键ID定位记录并更新所有字段。
     * 
     * 使用场景：修改帖子内容、状态等信息
     * 
     * @param post 要更新的帖子对象（必须包含有效的id）
     */
    @Update
    void updatePost(CommunityPost post);

    /**
     * 增加帖子的浏览次数
     * 
     * SQL说明：
     * - UPDATE ... SET viewCount = viewCount + 1: 原子操作，直接在数据库层面递增
     * - WHERE id = :id: 指定要更新的帖子
     * 
     * 性能优化：
     * - 无需先查询再更新，一次SQL完成
     * - 原子操作，线程安全
     * 
     * 使用场景：用户查看帖子详情时自动增加浏览计数
     * 
     * @param id 帖子ID
     */
    @Query("UPDATE community_posts SET viewCount = viewCount + 1 WHERE id = :id")
    void incrementViewCount(long id);

    /**
     * 关闭帖子（设置状态为'已关闭'）
     * 
     * SQL说明：
     * - UPDATE ... SET status = '已关闭': 直接修改状态字段
     * - WHERE id = :id: 指定要关闭的帖子
     * 
     * 使用场景：
     * - 帖子已过期
     * - 用户手动关闭
     * - 管理员审核不通过
     * 
     * @param id 帖子ID
     */
    @Query("UPDATE community_posts SET status = '已关闭' WHERE id = :id")
    void closePost(long id);

    /**
     * 删除帖子
     * 
     * Room 自动生成 DELETE 语句，根据主键ID删除记录。
     * 
     * 使用场景：
     * - 用户删除自己的帖子
     * - 管理员删除违规帖子
     * 
     * @param post 要删除的帖子对象（只需包含有效的id）
     */
    @Delete
    void deletePost(CommunityPost post);
}
