package com.udit.chatify.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.udit.chatify.R;
import com.udit.chatify.databinding.ActivityImageViewerBinding;

public class ImageViewerActivity extends AppCompatActivity {
    private ActivityImageViewerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve the image URL or path from the intent
        String imageUrl = getIntent().getStringExtra("image_url");
        String imageTitle = getIntent().getStringExtra("image_title");

        // Load the image using an image loading library like Glide or Picasso
        Glide.with(ImageViewerActivity.this)
                .load(imageUrl).placeholder(R.drawable.avatar)
                .into(binding.photoView);

        binding.imageTitle.setText(imageTitle);
        // Enable zooming functionality
        binding.photoView.setMaximumScale(5.0f);// Set the maximum scale level as desired
        binding.photoView.setOnViewTapListener((view, x, y) -> {
            // Implement any additional functionality on tap, if needed
        });
        binding.backButton.setOnClickListener(v -> onBackPressed());
    }
    @Override
    public void onBackPressed() {
        // Check if the zoom level is greater than 1.0
        if (binding.photoView.getScale() > 1.0f) {
            // Zoomed-in, so reset the zoom level
            binding.photoView.setScale(1.0f, true);
        } else {
            // Not zoomed-in, handle the back press normally
            super.onBackPressed();
        }
    }
}