package com.example.cookmate.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.cookmate.data.dao.CommunityPostDao;
import com.example.cookmate.data.database.AppDatabase;
import com.example.cookmate.data.model.CommunityPost;

import java.util.List;

public class CommunityRepository {

    private final CommunityPostDao communityPostDao;
    private final LiveData<List<CommunityPost>> allActivePosts;

    public CommunityRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        communityPostDao = db.communityPostDao();
        allActivePosts = communityPostDao.getAllActivePosts();
    }

    public LiveData<List<CommunityPost>> getAllActivePosts() {
        return allActivePosts;
    }

    public LiveData<List<CommunityPost>> getPostsByType(String type) {
        return communityPostDao.getPostsByType(type);
    }

    public CommunityPost getPostById(long id) {
        return communityPostDao.getPostById(id);
    }

    public long insertPost(CommunityPost post) {
        return communityPostDao.insertPost(post);
    }

    public void updatePost(CommunityPost post) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            communityPostDao.updatePost(post);
        });
    }

    public void incrementViewCount(long id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            communityPostDao.incrementViewCount(id);
        });
    }

    public void closePost(long id) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            communityPostDao.closePost(id);
        });
    }

    public void deletePost(CommunityPost post) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            communityPostDao.deletePost(post);
        });
    }
}