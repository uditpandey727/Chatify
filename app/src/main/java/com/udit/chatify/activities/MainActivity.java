package com.udit.chatify.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.udit.chatify.R;
import com.udit.chatify.databinding.ActivityMainBinding;
import com.udit.chatify.fragments.HomeFragment;
import com.udit.chatify.fragments.MainFragment;
import com.udit.chatify.fragments.SettingFragment;
import com.udit.chatify.fragments.UserFragment;
import com.udit.chatify.other.MyFirebaseManager;

import java.util.HashMap;
import java.util.Objects;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseDatabase database;

    // Fragment cache
    private final Fragment chatFragment = new MainFragment();
    private final Fragment homeFragment = new HomeFragment();
    private final Fragment profileFragment = new SettingFragment();
    private final Fragment userFragment = new UserFragment();
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception ignored) {}

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNavigationView.setSelectedItemId(R.id.chats); // set default tab once

        database = FirebaseDatabase.getInstance();
        MyFirebaseManager.firebaseRemoteConfig(this, binding.backgroundImage, false);
        MyFirebaseManager.setBottomNav(binding.bottomNavigationView, R.id.chats, R.drawable.chat_dark);

        initFragments();
        clickListener();
        firebaseManager();

        // Register callback for back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Are you sure you want to exit?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", (dialog, id) -> finish())
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        // Ask notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
    }

    private void initFragments() {
        FragmentManager fm = getSupportFragmentManager();

        // Add all fragments hidden first
        fm.beginTransaction()
                .add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
                .commit();

        fm.beginTransaction()
                .add(R.id.fragmentContainer, homeFragment, "home").hide(homeFragment)
                .commit();

        fm.beginTransaction()
                .add(R.id.fragmentContainer, chatFragment, "chat").hide(chatFragment)
                .commit();

        // Show only Chats as default
        fm.beginTransaction()
                .show(chatFragment)
                .commit();

        activeFragment = chatFragment;
    }


    private void clickListener() {
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.chats) {
                switchFragment(chatFragment);
                return true;
            } else if (itemId == R.id.home) {
                switchFragment(homeFragment);
                return true;
            } else if (itemId == R.id.profile) {
                switchFragment(profileFragment);
                return true;
            } else if (itemId == R.id.users) {
                switchFragment(userFragment);
                return true;
            }
            return false;
        });
    }


    private void switchFragment(Fragment targetFragment) {
        if (targetFragment != activeFragment) {
            getSupportFragmentManager().beginTransaction()
                    .hide(activeFragment)
                    .show(targetFragment)
                    .commit();
            activeFragment = targetFragment;
        }
    }

    private void firebaseManager() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            HashMap<String, Object> map = new HashMap<>();
            map.put("token", token);
            database.getReference()
                    .child("users")
                    .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                    .updateChildren(map);
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        if (currentId != null) {
            database.getReference().child("presence").child(currentId).setValue("Online");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        String currentId = FirebaseAuth.getInstance().getUid();
        if (currentId != null) {
            database.getReference().child("presence").child(currentId).setValue("Offline");
        }
    }
}
