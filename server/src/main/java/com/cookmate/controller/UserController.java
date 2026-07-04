package com.cookmate.controller;

import com.cookmate.entity.FoodItem;
import com.cookmate.entity.Recipe;
import com.cookmate.entity.User;
import com.cookmate.repository.FoodItemRepository;
import com.cookmate.repository.RecipeRepository;
import com.cookmate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @PostMapping("/user/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();

        if (userRepository.findByUsername(user.getUsername()) != null) {
            result.put("code", 400);
            result.put("message", "用户名已存在");
            return result;
        }

        User saved = userRepository.save(user);
        result.put("code", 200);
        result.put("message", "注册成功");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", saved.getId());
        data.put("username", saved.getUsername());
        result.put("data", data);
        return result;
    }

    @PostMapping("/user/login")
    public Map<String, Object> login(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        User found = userRepository.findByUsername(user.getUsername());
        if (found == null) {
            result.put("code", 400);
            result.put("message", "用户不存在");
            return result;
        }
        if (!found.getPassword().equals(user.getPassword())) {
            result.put("code", 400);
            result.put("message", "密码错误");
            return result;
        }
        result.put("code", 200);
        result.put("message", "登录成功");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", found.getId());
        data.put("username", found.getUsername());
        result.put("data", data);
        return result;
    }

    @DeleteMapping("/user/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            List<FoodItem> foods = foodItemRepository.findByUserId(id);
            foodItemRepository.deleteAll(foods);
            List<Recipe> recipes = recipeRepository.findByUserIdOrderByCreatedAtDesc(id);
            recipeRepository.deleteAll(recipes);
            userRepository.deleteById(id);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "注销成功");
            return result;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("message", "用户不存在");
        return result;
    }
}