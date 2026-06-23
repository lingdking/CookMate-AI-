package com.example.cookmate.ui.inventory;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.cookmate.data.database.AppDatabase;
import com.example.cookmate.data.model.FoodItem;
import com.example.cookmate.data.repository.FoodRepository;

import java.util.List;

/**
 * 库存管理ViewModel
 * 负责管理食材数据的业务逻辑，作为Fragment和数据层之间的桥梁
 * 使用LiveData实现数据自动更新，遵循MVVM架构模式
 */
public class InventoryViewModel extends AndroidViewModel {

    // 食材数据仓库，负责数据操作
    private final FoodRepository repository;
    // 活跃食材列表的LiveData（未消耗的食材）
    private final LiveData<List<FoodItem>> allActiveItems;
    // 已消耗食材列表的LiveData（被标记为已吃掉的食材）
    private final LiveData<List<FoodItem>> allConsumedItems;

    /**
     * ViewModel构造函数
     * 初始化数据仓库和LiveData观察者
     * @param application 应用程序上下文
     */
    public InventoryViewModel(Application application) {
        super(application);
        // 创建数据仓库实例
        repository = new FoodRepository(application);
        // 获取活跃食材的LiveData流
        allActiveItems = repository.getAllActiveItems();
        // 获取已消耗食材的LiveData流
        allConsumedItems = repository.getAllConsumedItems();
    }

    /**
     * 获取所有活跃食材的LiveData
     * UI层可以观察此LiveData以自动更新界面
     * @return 活跃食材列表的LiveData
     */
    public LiveData<List<FoodItem>> getAllActiveItems() {
        return allActiveItems;
    }

    /**
     * 获取所有已消耗食材的LiveData
     * UI层可以观察此LiveData以自动更新界面
     * @return 已消耗食材列表的LiveData
     */
    public LiveData<List<FoodItem>> getAllConsumedItems() {
        return allConsumedItems;
    }

    /**
     * 将食材标记为已消耗
     * @param id 食材ID
     * @param method 消耗方式（如：吃掉、丢弃等）
     */
    public void markAsConsumed(long id, String method) {
        repository.markAsConsumed(id, method);
    }
    
    /**
     * 删除指定的食材项
     * 在后台线程执行数据库操作，避免阻塞主线程
     * @param id 要删除的食材ID
     */
    public void deleteItem(long id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 先查询获取完整的FoodItem对象
            FoodItem item = repository.getItemById(id);
            if (item != null) {
                repository.deleteItem(item);
            }
        });
    }
    
    /**
     * 清空所有已消耗的食材记录
     * 在后台线程执行批量删除操作
     */
    public void clearConsumed() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 同步获取所有已消耗的食材
            List<FoodItem> consumed = AppDatabase.getInstance(getApplication())
                    .foodItemDao().getAllConsumedItemsSync();
            if (consumed != null) {
                // 逐个删除已消耗的食材
                for (FoodItem item : consumed) {
                    AppDatabase.getInstance(getApplication()).foodItemDao().deleteItem(item);
                }
            }
        });
    }

    /**
     * 清空所有食材（包括活跃和已消耗的）
     * 危险操作，会删除用户的所有食材数据
     * 在后台线程执行批量删除操作
     */
    public void clearAll() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 同步获取所有活跃的食材
            List<FoodItem> all = AppDatabase.getInstance(getApplication())
                    .foodItemDao().getAllActiveItemsSync();
            if (all != null) {
                // 逐个删除所有食材
                for (FoodItem item : all) {
                    AppDatabase.getInstance(getApplication()).foodItemDao().deleteItem(item);
                }
            }
        });
    }
}