package com.example.cookmate.ui.community;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.cookmate.data.model.CommunityPost;
import com.example.cookmate.data.repository.CommunityRepository;

import java.util.List;

public class CommunityViewModel extends AndroidViewModel {

    private final CommunityRepository repository;
    private final LiveData<List<CommunityPost>> allActivePosts;

    public CommunityViewModel(Application application) {
        super(application);
        repository = new CommunityRepository(application);
        allActivePosts = repository.getAllActivePosts();
    }

    public LiveData<List<CommunityPost>> getAllActivePosts() {
        return allActivePosts;
    }

    public LiveData<List<CommunityPost>> getPostsByType(String type) {
        return repository.getPostsByType(type);
    }

    public long insertPost(CommunityPost post) {
        return repository.insertPost(post);
    }

    public void closePost(long id) {
        repository.closePost(id);
    }
}