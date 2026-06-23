
package com.example.cookmate.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cookmate.data.model.Recipe;

import java.util.List;

/**
 * 菜谱数据访问对象（DAO）
 * 
 * 定义了对 recipes 表的所有数据库操作。
 * Room 框架会在编译时自动生成实现类（RecipeDao_Impl）。
 * 
 * 主要功能：
 * - 管理用户菜谱的增删改查
 * - 支持收藏功能和标记已做过
 * - 按类型筛选菜谱（正好用完/需补1-2样/只能消耗部分）
 * - 提供响应式查询（LiveData）和同步查询
 * 
 * 数据库表结构：
 * - id: 主键，自增
 * - name: 菜谱名称
 * - usedFoodItemIds: 使用的食材ID列表（逗号分隔）
 * - additionalIngredients: 需要额外购买的食材
 * - recipeType: 菜谱类型（正好用完/需补1-2样/只能消耗部分）
 * - steps: 烹饪步骤详情
 * - cookingTime: 烹饪时长（分钟）
 * - difficulty: 难度等级（简单/中等/困难）
 * - description: 菜谱描述
 * - isCooked: 是否做过
 * - isFavorite: 是否收藏
 * - createdAt: 创建时间
 */
@Dao
public interface RecipeDao {

    /**
     * 获取所有菜谱（响应式）
     * 
     * SQL说明：
     * - ORDER BY createdAt DESC: 按创建时间倒序，最新创建的菜谱在前
     * 
     * 使用场景：RecipeFragment 中显示所有菜谱列表
     * 
     * @return LiveData<List<Recipe>> 菜谱列表的响应式观察者
     */
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    LiveData<List<Recipe>> getAllRecipes();

    /**
     * 获取收藏的菜谱（响应式）
     * 
     * SQL说明：
     * - WHERE isFavorite = 1: 只查询已收藏的菜谱
     * - ORDER BY createdAt DESC: 按创建时间倒序
     * 
     * 使用场景：显示用户收藏的菜谱列表
     * 
     * @return LiveData<List<Recipe>> 收藏菜谱列表的响应式观察者
     */
    @Query("SELECT * FROM recipes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    LiveData<List<Recipe>> getFavoriteRecipes();

    /**
     * 根据菜谱类型获取菜谱（响应式）
     * 
     * SQL说明：
     * - WHERE recipeType = :type: 过滤指定类型的菜谱
     * - ORDER BY createdAt DESC: 按创建时间倒序
     * 
     * 菜谱类型说明：
     * - "正好用完": 完全使用现有食材
     * - "需补1-2样": 需要购买少量食材
     * - "只能消耗部分": 只能使用部分现有食材
     * 
     * 使用场景：按分类筛选菜谱
     * 
     * @param type 菜谱类型
     * @return LiveData<List<Recipe>> 指定类型菜谱列表的响应式观察者
     */
    @Query("SELECT * FROM recipes WHERE recipeType = :type ORDER BY createdAt DESC")
    LiveData<List<Recipe>> getRecipesByType(String type);

    /**
     * 根据ID获取单个菜谱
     * 
     * 注意：这是同步方法，返回单个对象而非LiveData。
     * 需要在子线程调用。
     * 
     * 使用场景：查看菜谱详情
     * 
     * @param id 菜谱ID
     * @return Recipe 菜谱对象（如果不存在则返回null）
     */
    @Query("SELECT * FROM recipes WHERE id = :id")
    Recipe getRecipeById(long id);

    /**
     * 插入菜谱
     * 
     * 冲突策略：OnConflictStrategy.REPLACE
     * - 如果主键或唯一约束冲突，则替换已有记录
     * - 返回插入的行ID
     * 
     * 使用场景：
     * - 保存AI生成的菜谱
     * - 手动添加自定义菜谱
     * 
     * @param recipe 要插入的菜谱对象
     * @return long 插入的行ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertRecipe(Recipe recipe);

    /**
     * 更新菜谱信息
     * 
     * Room 根据主键ID定位记录并更新所有字段。
     * 
     * 使用场景：修改菜谱名称、步骤等信息
     * 
     * @param recipe 要更新的菜谱对象（必须包含有效的id）
     */
    @Update
    void updateRecipe(Recipe recipe);

    /**
     * 切换菜谱的收藏状态
     * 
     * SQL说明：
     * - CASE WHEN isFavorite = 0 THEN 1 ELSE 0 END: 
     *   如果当前未收藏(0)则设为收藏(1)，反之取消收藏(0)
     * - 这是一个原子操作，无需先查询再更新
     * 
     * 性能优势：
     * - 一次SQL完成状态切换
     * - 线程安全，避免并发问题
     * 
     * 使用场景：用户点击收藏/取消收藏按钮
     * 
     * @param id 菜谱ID
     */
    @Query("UPDATE recipes SET isFavorite = CASE WHEN isFavorite = 0 THEN 1 ELSE 0 END WHERE id = :id")
    void toggleFavorite(long id);

    /**
     * 标记菜谱为已做过
     * 
     * SQL说明：
     * - UPDATE ... SET isCooked = 1: 将isCooked字段设为1（true）
     * - WHERE id = :id: 指定要更新的菜谱
     * 
     * 使用场景：用户完成烹饪后标记该菜谱
     * 
     * 注意：此操作不可逆（没有取消标记的方法）
     * 
     * @param id 菜谱ID
     */
    @Query("UPDATE recipes SET isCooked = 1 WHERE id = :id")
    void markAsCooked(long id);

    /**
     * 删除菜谱
     * 
     * Room 自动生成 DELETE 语句，根据主键ID删除记录。
     * 
     * 使用场景：
     * - 用户删除不需要的菜谱
     * - 清理测试数据
     * 
     * @param recipe 要删除的菜谱对象（只需包含有效的id）
     */
    @Delete
    void deleteRecipe(Recipe recipe);
}

