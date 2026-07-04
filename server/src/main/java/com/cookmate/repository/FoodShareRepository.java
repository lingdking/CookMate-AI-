package com.cookmate.repository;

import com.cookmate.entity.FoodShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodShareRepository extends JpaRepository<FoodShare, Long> {
    List<FoodShare> findByStatusOrderByCreatedAtDesc(String status);
}