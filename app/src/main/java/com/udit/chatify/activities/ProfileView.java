package com.udit.chatify.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.bumptech.glide.Glide;
import com.udit.chatify.R;
import com.udit.chatify.databinding.ActivityProfileViewBinding;

public class ProfileView extends AppCompatActivity {
    ActivityProfileViewBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String name = getIntent().getStringExtra("name");
        String profile = getIntent().getStringExtra("profile");
        String bio = getIntent().getStringExtra("bio");
        String phoneNumber = getIntent().getStringExtra("phoneNumber");

        if(phoneNumber.equals("isPublic")){
            binding.phoneNumber.setVisibility(View.GONE);
        }else {
            binding.phoneNumber.setText(phoneNumber);
        }
        binding.backImg.setOnClickListener(v -> finish());

        binding.name.setText(name);
        binding.bioText.setText(bio);

        Glide.with(ProfileView.this).load(profile)
                .placeholder(R.drawable.avatar)
                .into(binding.profileImage);
        binding.profileImage.setOnClickListener(v -> {
            // Launch ImageViewerActivity and pass the image URL or path
            Intent intent = new Intent(ProfileView.this, ImageViewerActivity.class);
            intent.putExtra("image_url", profile);
            startActivity(intent);
        });
    }
}