package com.cookmate.controller;

import com.cookmate.entity.CommunityPost;
import com.cookmate.repository.CommunityPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class CommunityPostController {
    @Autowired
    private CommunityPostRepository postRepository;

    @GetMapping("/posts")
    public Map<String, Object> getAll() {
        List<CommunityPost> posts = postRepository.findAllByOrderByCreatedAtDesc();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", posts);
        return result;
    }

    @PostMapping("/posts")
    public Map<String, Object> add(@RequestBody CommunityPost post) {
        postRepository.save(post);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "发布成功");
        return result;
    }
    @DeleteMapping("/posts/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        postRepository.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }
}