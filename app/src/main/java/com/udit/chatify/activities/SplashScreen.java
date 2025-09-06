package com.udit.chatify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.udit.chatify.databinding.ActivitySplashScreenBinding;

public class SplashScreen extends AppCompatActivity {
    ActivitySplashScreenBinding binding;
    FirebaseAuth auth;

    private static final int SPLASH_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        if(auth.getCurrentUser() != null) {
            new Handler().postDelayed(() -> {
                Intent mainIntent = new Intent(SplashScreen.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }, SPLASH_DURATION);
        }else{
            new Handler().postDelayed(() -> {
                Intent mainIntent = new Intent(SplashScreen.this, PhoneNumberActivity.class);
                startActivity(mainIntent);
                finish();
            }, SPLASH_DURATION+1000);}
    }
}