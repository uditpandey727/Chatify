package com.udit.chatify.other;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.udit.chatify.R;

public class MyFirebaseManager {

    public static void firebaseRemoteConfig(Context context, ImageView image, boolean isChat){
        // Firebase  Remote Config
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        mFirebaseRemoteConfig.fetchAndActivate().addOnSuccessListener(aBoolean -> {
            String backgroundImage;
            // Update UI based on remote config values
            if(isChat){
                backgroundImage = mFirebaseRemoteConfig.getString("chatBackground");
            }else{
                backgroundImage = mFirebaseRemoteConfig.getString("mainBackground");
            }
            Glide.with(context)
                    .load(backgroundImage)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(image);
        });
    }

    public static void setBottomNav(BottomNavigationView bottomNav, int selectedId, int selectedImage){
        // Setting Bottom Nav
        Menu menu = bottomNav.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            int id = menuItem.getItemId();
            if(id == selectedId){
                menuItem.setIcon(selectedImage);
            }
            else{
                if(id == R.id.home){
                    menuItem.setIcon(R.drawable.home);
                }if(id == R.id.profile) {
                    menuItem.setIcon(R.drawable.user);
                }
                if(id == R.id.chats) {
                    menuItem.setIcon(R.drawable.chat);
                }
                if(id == R.id.ai) {
                    menuItem.setIcon(R.drawable.ai);
                }
            }
        }
    }
}
