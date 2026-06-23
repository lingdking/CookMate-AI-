package com.example.cookmate.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.cookmate.data.dao.FoodItemDao;
import com.example.cookmate.data.database.AppDatabase;
import com.example.cookmate.data.model.FoodItem;
import com.example.cookmate.util.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FoodRepository {

    private final FoodItemDao foodItemDao;
    private final LiveData<List<FoodItem>> allActiveItems;
    private final LiveData<Integer> activeItemCount;

    public LiveData<List<FoodItem>> getAllConsumedItems() {
        return foodItemDao.getAllConsumedItems();
    }

    public FoodRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        foodItemDao = db.foodItemDao();
        allActiveItems = foodItemDao.getAllActiveItems();
        activeItemCount = foodItemDao.getActiveItemCount();
    }

    public LiveData<List<FoodItem>> getAllActiveItems() {
        return allActiveItems;
    }

    public LiveData<Integer> getActiveItemCount() {
        return activeItemCount;
    }

    public LiveData<List<FoodItem>> getExpiringSoonItems() {
        Date now = DateUtils.today();
        Date threeDaysLater = DateUtils.threeDaysLater();
        return foodItemDao.getExpiringSoonItems(now, threeDaysLater);
    }

    public LiveData<List<FoodItem>> getItemsByCategory(String category) {
        return foodItemDao.getItemsByCategory(category);
    }

    public FoodItem getItemById(long id) {
        return foodItemDao.getItemById(id);
    }

    public LiveData<FoodItem> getItemByIdLive(long id) {
        return foodItemDao.getItemByIdLive(id);
    }

    public long insertItem(FoodItem item) {
        if (item.getExpiryDate() == null || item.getExpiryDate().equals(item.getPurchaseDate())) {
            Date expiryDate = DateUtils.estimateExpiryDate(item.getPurchaseDate(), item.getCategory());
            item.setExpiryDate(expiryDate);
        }
        item.setStatus(DateUtils.getFoodStatus(item.getExpiryDate()));

        final long[] result = new long[1];
        try {
            AppDatabase.databaseWriteExecutor.submit(() -> {
                result[0] = foodItemDao.insertItem(item);
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result[0];
    }

    public void insertItems(List<FoodItem> items) {
        for (FoodItem item : items) {
            if (item.getExpiryDate() == null || item.getExpiryDate().equals(item.getPurchaseDate())) {
                Date expiryDate = DateUtils.estimateExpiryDate(item.getPurchaseDate(), item.getCategory());
                item.setExpiryDate(expiryDate);
            }
            item.setStatus(DateUtils.getFoodStatus(item.getExpiryDate()));
        }
        AppDatabase.databaseWriteExecutor.execute(() -> {
            foodItemDao.insertItems(items);
        });
    }

    public void updateItem(FoodItem item) {
        item.setStatus(DateUtils.getFoodStatus(item.getExpiryDate()));
        AppDatabase.databaseWriteExecutor.execute(() -> {
            foodItemDao.updateItem(item);
        });
    }

    public void markAsConsumed(long id, String method) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            foodItemDao.markAsConsumed(id, new Date(), method);
        });
    }

    public void deleteItem(FoodItem item) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            foodItemDao.deleteItem(item);
        });
    }

    public List<FoodItem> getAllActiveItemsSync() {
        try {
            return AppDatabase.databaseWriteExecutor.submit(() ->
                    foodItemDao.getAllActiveItemsSync()
            ).get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}