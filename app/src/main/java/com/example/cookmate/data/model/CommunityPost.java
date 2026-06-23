package com.example.cookmate.data.model;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

    @Entity(tableName = "community_posts")
    public class CommunityPost {

        @PrimaryKey(autoGenerate = true)
        private long id;

        private String postType;             // 赠送、交换、求助
        private Long foodItemId;             // 关联的食材ID（可空）
        private String title;                // 标题
        private String content;              // 内容描述
        private String imagePaths;           // 图片路径，逗号分隔
        private String userId;               // 用户ID
        private String userName;             // 用户昵称
        private double longitude;            // 经度
        private double latitude;             // 纬度
        private String locationDescription;  // 位置描述
        private String status;               // 有效、已处理、已关闭
        private int viewCount;               // 查看次数
        private int adoptCount;              // 被采纳次数
        private Date createdAt;              // 发布时间
        private Date expiryTime;             // 过期时间

        public CommunityPost() {
            this.createdAt = new Date();
            this.status = "有效";
            this.viewCount = 0;
            this.adoptCount = 0;
            this.userName = "匿名用户";
            this.postType = "赠送";
        }

        // ========== Getter 和 Setter ==========
        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getPostType() { return postType; }
        public void setPostType(String postType) { this.postType = postType; }

        public Long getFoodItemId() { return foodItemId; }
        public void setFoodItemId(Long foodItemId) { this.foodItemId = foodItemId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getImagePaths() { return imagePaths; }
        public void setImagePaths(String imagePaths) { this.imagePaths = imagePaths; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public String getLocationDescription() { return locationDescription; }
        public void setLocationDescription(String locationDescription) { this.locationDescription = locationDescription; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getViewCount() { return viewCount; }
        public void setViewCount(int viewCount) { this.viewCount = viewCount; }

        public int getAdoptCount() { return adoptCount; }
        public void setAdoptCount(int adoptCount) { this.adoptCount = adoptCount; }

        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

        public Date getExpiryTime() { return expiryTime; }
        public void setExpiryTime(Date expiryTime) { this.expiryTime = expiryTime; }
    }

