package com.example.cookmate.data.model;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

    @Entity(tableName = "recipes")
    public class Recipe {

        @PrimaryKey(autoGenerate = true)
        private long id;

        private String name;                // 菜谱名称
        private String usedFoodItemIds;     // 使用的食材ID，逗号分隔
        private String additionalIngredients; // 需要额外采购的食材
        private String recipeType;          // 正好用完、需补1-2样、只能消耗部分
        private String steps;               // 烹饪步骤
        private int cookingTime;            // 烹饪时间（分钟）
        private String difficulty;          // 简单、中等、困难
        private String description;         // 菜谱描述
        private boolean isCooked;           // 是否做过
        private boolean isFavorite;         // 是否收藏
        private Date createdAt;             // 生成时间

        public Recipe() {
            this.createdAt = new Date();
            this.recipeType = "正好用完";
            this.difficulty = "简单";
            this.cookingTime = 30;
            this.isCooked = false;
            this.isFavorite = false;
        }

        // ========== Getter 和 Setter ==========
        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUsedFoodItemIds() { return usedFoodItemIds; }
        public void setUsedFoodItemIds(String usedFoodItemIds) { this.usedFoodItemIds = usedFoodItemIds; }

        public String getAdditionalIngredients() { return additionalIngredients; }
        public void setAdditionalIngredients(String additionalIngredients) { this.additionalIngredients = additionalIngredients; }

        public String getRecipeType() { return recipeType; }
        public void setRecipeType(String recipeType) { this.recipeType = recipeType; }

        public String getSteps() { return steps; }
        public void setSteps(String steps) { this.steps = steps; }

        public int getCookingTime() { return cookingTime; }
        public void setCookingTime(int cookingTime) { this.cookingTime = cookingTime; }

        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isCooked() { return isCooked; }
        public void setCooked(boolean cooked) { isCooked = cooked; }

        public boolean isFavorite() { return isFavorite; }
        public void setFavorite(boolean favorite) { isFavorite = favorite; }

        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    }

