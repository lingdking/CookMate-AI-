
package com.example.cookmate.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cookmate.data.model.FoodItem;

import java.util.Date;
import java.util.List;

    @Dao
    public interface FoodItemDao {

        // 获取所有未消耗的食材，按过期日期升序
        @Query("SELECT * FROM food_items WHERE isConsumed = 0 ORDER BY expiryDate ASC")
        LiveData<List<FoodItem>> getAllActiveItems();

        // 获取所有已消耗的食材
        @Query("SELECT * FROM food_items WHERE isConsumed = 1 ORDER BY consumedAt DESC")
        LiveData<List<FoodItem>> getAllConsumedItems();

        // 根据ID获取单个食材
        @Query("SELECT * FROM food_items WHERE id = :id")
        FoodItem getItemById(long id);

        // 根据ID获取单个食材（LiveData版本）
        @Query("SELECT * FROM food_items WHERE id = :id")
        LiveData<FoodItem> getItemByIdLive(long id);

        // 获取临近过期的食材（3天内）
        @Query("SELECT * FROM food_items WHERE isConsumed = 0 AND expiryDate BETWEEN :now AND :threeDaysLater ORDER BY expiryDate ASC")
        LiveData<List<FoodItem>> getExpiringSoonItems(Date now, Date threeDaysLater);

        // 根据类别获取食材
        @Query("SELECT * FROM food_items WHERE category = :category AND isConsumed = 0")
        LiveData<List<FoodItem>> getItemsByCategory(String category);

        // 插入食材，返回插入的ID
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long insertItem(FoodItem item);

        // 批量插入
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        long[] insertItems(List<FoodItem> items);

        // 更新食材
        @Update
        void updateItem(FoodItem item);

        // 标记为已消耗
        @Query("UPDATE food_items SET isConsumed = 1, consumedAt = :consumedAt, consumeMethod = :method, status = '已消耗' WHERE id = :id")
        void markAsConsumed(long id, Date consumedAt, String method);

        // 删除食材
        @Delete
        void deleteItem(FoodItem item);
        @Query("SELECT * FROM food_items WHERE isConsumed = 1")
        List<FoodItem> getAllConsumedItemsSync();

        @Query("SELECT * FROM food_items")
        List<FoodItem> getAllItemsSync();
        // 获取未消耗食材总数
        @Query("SELECT COUNT(*) FROM food_items WHERE isConsumed = 0")
        LiveData<Integer> getActiveItemCount();

        @Query("SELECT * FROM food_items WHERE isConsumed = 0")
        List<FoodItem> getAllActiveItemsSync();


    }

