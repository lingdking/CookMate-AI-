package com.cookmate.controller;

import com.cookmate.entity.FoodShare;
import com.cookmate.repository.FoodShareRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FoodShareController {

    @Autowired
    private FoodShareRepository foodShareRepository;

    @GetMapping("/shares")
    public Map<String, Object> getShares() {
        List<FoodShare> shares = foodShareRepository.findByStatusOrderByCreatedAtDesc("available");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", shares);
        return result;
    }

    @PostMapping("/shares")
    public Map<String, Object> addShare(@RequestBody FoodShare share) {
        FoodShare saved = foodShareRepository.save(share);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "发布成功");
        result.put("data", Map.of("id", saved.getId()));
        return result;
    }
}