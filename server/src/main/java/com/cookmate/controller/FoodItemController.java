package com.cookmate.controller;

import com.cookmate.entity.FoodItem;
import com.cookmate.repository.FoodItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FoodItemController {

    @Autowired
    private FoodItemRepository foodItemRepository;
//
    @GetMapping("/food/{userId}")
    public Map<String, Object> getActive(@PathVariable Long userId) {
        List<FoodItem> items = foodItemRepository.findByUserIdAndIsConsumedFalseOrderByExpiryDateAsc(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", items);
        return result;
    }

    @GetMapping("/food/consumed/{userId}")
    public Map<String, Object> getConsumed(@PathVariable Long userId) {
        List<FoodItem> items = foodItemRepository.findByUserIdAndIsConsumedTrue(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", items);
        return result;
    }

    @PostMapping("/food")
    public Map<String, Object> add(@RequestBody FoodItem item) {
        foodItemRepository.save(item);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "添加成功");
        return result;
    }

    @PutMapping("/food/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody FoodItem item) {
        item.setId(id);
        foodItemRepository.save(item);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    @DeleteMapping("/food/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        foodItemRepository.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }

    @DeleteMapping("/food/clear/{userId}")
    public Map<String, Object> clearAll(@PathVariable Long userId) {
        List<FoodItem> items = foodItemRepository.findByUserId(userId);
        foodItemRepository.deleteAll(items);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已清空");
        return result;
    }
}