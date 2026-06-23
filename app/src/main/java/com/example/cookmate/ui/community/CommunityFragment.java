package com.example.cookmate.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.R;
import com.example.cookmate.network.ServerClient;
import com.example.cookmate.ui.recipe.RecipeDetailActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends Fragment {

    private RecyclerView rvPosts;
    private PostAdapter adapter;
    private List<PostItem> posts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);
        rvPosts = view.findViewById(R.id.rv_posts);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PostAdapter();
        rvPosts.setAdapter(adapter);

        view.findViewById(R.id.btn_add_post).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CreatePostActivity.class))
        );

        loadPosts();
        return view;
    }

    @Override
    public void onResume() { super.onResume(); loadPosts(); }

    private void loadPosts() {
        ServerClient.getPosts(new ServerClient.OnResultListener() {
            @Override
            public void onSuccess(String json) {
                posts.clear();
                try {
                    JSONArray data = new JSONObject(json).getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        PostItem item = new PostItem();
                        item.id = obj.getLong("id");
                        item.userId = obj.getLong("userId");
                        item.title = obj.getString("title");
                        item.content = obj.optString("content", "");
                        item.username = obj.optString("username", "匿名");
                        item.createdAt = obj.optString("createdAt", "");
                        item.recipeSteps = obj.optString("recipeSteps", "");
                        posts.add(item);
                    }
                } catch (Exception e) { e.printStackTrace(); }
                adapter.notifyDataSetChanged();
            }
            @Override public void onError(String e) { safeToast("加载失败"); }
        });
    }

    private void safeToast(String msg) { if (isAdded() && getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show(); }

    private class PostItem {
        long id;
        long userId;
        String recipeSteps;
        String title, content, username, createdAt;
    }

    private class PostAdapter extends RecyclerView.Adapter<PostAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_post, p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            PostItem item = posts.get(pos);
            h.tvTitle.setText(item.title);
            h.tvContent.setText(item.content);
            h.tvUser.setText(item.username + " · " + item.createdAt);

            if (item.recipeSteps != null && !item.recipeSteps.isEmpty()) {
                h.tvRecipe.setVisibility(View.VISIBLE);
                h.tvRecipe.setText("📋 附带菜谱（点击查看）");
                h.tvRecipe.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), RecipeDetailActivity.class);
                    intent.putExtra("recipe_name", item.title);
                    intent.putExtra("recipe_steps", item.recipeSteps);
                    startActivity(intent);
                });
            } else {
                h.tvRecipe.setVisibility(View.GONE);
            }

            long currentUserId = requireContext().getSharedPreferences("user", 0).getLong("userId", -1);
            h.tvDelete.setVisibility(item.userId == currentUserId ? View.VISIBLE : View.GONE);
            h.tvDelete.setOnClickListener(v -> {
                ServerClient.deletePost(item.id, new ServerClient.OnResultListener() {
                    @Override public void onSuccess(String json) {
                        posts.remove(pos);
                        notifyItemRemoved(pos);
                    }
                    @Override public void onError(String error) { safeToast("删除失败"); }
                });
            });
        }

        @Override public int getItemCount() { return posts.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvContent, tvUser, tvDelete, tvRecipe;
            VH(@NonNull View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tv_post_title);
                tvContent = v.findViewById(R.id.tv_post_content);
                tvUser = v.findViewById(R.id.tv_post_user);
                tvDelete = v.findViewById(R.id.tv_delete_post);
                tvRecipe = v.findViewById(R.id.tv_post_recipe);
            }
        }
    }
}