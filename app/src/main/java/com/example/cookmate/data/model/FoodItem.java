package com.example.cookmate.data.model;


import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

    @Entity(tableName = "food_items")
    public
    class FoodItem {

        @PrimaryKey(autoGenerate = true)
        private long id;

        private String name;          // 食材名称
        private String category;      // 类别：蔬菜、肉类、水果、调料、乳制品、其他
        private String quantity;      // 数量描述，如"2个"、"300g"
        private Date purchaseDate;    // 购买日期
        private Date expiryDate;      // 过期日期
        private String status;        // 新鲜、临近过期、已过期、已消耗
        private String imagePath;     // 食材图片路径
        private String inputMethod;   // 录入方式：手动、拍照、语音、小票
        private String notes;         // 备注
        private Date createdAt;       // 创建时间
        private boolean isConsumed;   // 是否已消耗
        private Date consumedAt;      // 消耗日期
        private String consumeMethod; // 消耗方式：吃掉、送出、扔掉

        // ========== 构造函数 ==========
        public FoodItem() {
            this.purchaseDate = new Date();
            this.expiryDate = new Date();
            this.createdAt = new Date();
            this.status = "新鲜";
            this.category = "其他";
            this.inputMethod = "手动";
            this.isConsumed = false;
        }
        @Ignore
        public FoodItem(String name, String category, String quantity, Date expiryDate) {
            this();
            this.name = name;
            this.category = category;
            this.quantity = quantity;
            this.expiryDate = expiryDate;
        }

        // ========== Getter 和 Setter ==========
        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getQuantity() { return quantity; }
        public void setQuantity(String quantity) { this.quantity = quantity; }

        public Date getPurchaseDate() { return purchaseDate; }
        public void setPurchaseDate(Date purchaseDate) { this.purchaseDate = purchaseDate; }

        public Date getExpiryDate() { return expiryDate; }
        public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }

        public String getInputMethod() { return inputMethod; }
        public void setInputMethod(String inputMethod) { this.inputMethod = inputMethod; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

        public boolean isConsumed() { return isConsumed; }
        public void setConsumed(boolean consumed) { isConsumed = consumed; }

        public Date getConsumedAt() { return consumedAt; }
        public void setConsumedAt(Date consumedAt) { this.consumedAt = consumedAt; }

        public String getConsumeMethod() { return consumeMethod; }
        public void setConsumeMethod(String consumeMethod) { this.consumeMethod = consumeMethod; }
    }

