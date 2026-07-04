package com.cookmate.repository;

import com.cookmate.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    // 这个方法名会被Spring Data JPA自动解析成SQL
    List<FoodItem> findByUserIdAndIsConsumedFalseOrderByExpiryDateAsc(Long userId);
    List<FoodItem> findByUserIdAndIsConsumedTrue(Long userId);
    // 查询某个用户的全部食材
    List<FoodItem> findByUserId(Long userId);
}