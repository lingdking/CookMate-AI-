package com.example.cookmate;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.cookmate.databinding.ActivityMainBinding;
import com.example.cookmate.network.AIManager;
import com.example.cookmate.ui.ai.AIFragment;
import com.example.cookmate.ui.community.CommunityFragment;
import com.example.cookmate.ui.inventory.InventoryFragment;
import com.example.cookmate.ui.profile.ProfileFragment;
import com.example.cookmate.ui.recipe.RecipeFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Fragment currentFragment;
    private AIFragment aiFragment;
    private InventoryFragment inventoryFragment;
    private RecipeFragment recipeFragment;
    private CommunityFragment communityFragment;
    private ProfileFragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AIManager.init("sk-5a9a880ffc7f42a9a61e5b7f015594a3");

        aiFragment = new AIFragment();
        inventoryFragment = new InventoryFragment();
        recipeFragment = new RecipeFragment();
        communityFragment = new CommunityFragment();
        profileFragment = new ProfileFragment();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, inventoryFragment, "inventory")
                    .add(R.id.fragment_container, recipeFragment, "recipe")
                    .add(R.id.fragment_container, communityFragment, "community")
                    .add(R.id.fragment_container, aiFragment, "ai")
                    .add(R.id.fragment_container, profileFragment, "profile")
                    .hide(recipeFragment).hide(communityFragment).hide(aiFragment).hide(profileFragment)
                    .commit();
            currentFragment = inventoryFragment;
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment target = null;
            int id = item.getItemId();
            if (id == R.id.nav_inventory) target = inventoryFragment;
            else if (id == R.id.nav_recipe) target = recipeFragment;
            else if (id == R.id.nav_community) target = communityFragment;
            else if (id == R.id.nav_ai) target = aiFragment;
            else if (id == R.id.nav_profile) target = profileFragment;

            if (target != null && target != currentFragment) {
                getSupportFragmentManager().beginTransaction()
                        .hide(currentFragment).show(target).commit();
                currentFragment = target;
            }
            return true;
        });
    }
}