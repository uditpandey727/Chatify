package com.udit.chatify.activities;

import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;

import androidx.appcompat.app.AppCompatActivity;

import com.udit.chatify.databinding.ActivityVideoViewerBinding;

public class VideoViewerActivity extends AppCompatActivity {

    private ActivityVideoViewerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get the video URL from the intent
        String videoUrl = getIntent().getStringExtra("video_url");

        // Set the video URI
        binding.videoView.setVideoURI(Uri.parse(videoUrl));

        // Add media controller
        MediaController mediaController = new MediaController(this);
        binding.videoView.setMediaController(mediaController);
        mediaController.setAnchorView(binding.videoView);

        // Start playing the video
        binding.videoView.start();

        // Handle video completion
        binding.videoView.setOnCompletionListener(mediaPlayer -> {
            // Video playback has completed
            // You can perform any additional logic here
        });
    }
}
