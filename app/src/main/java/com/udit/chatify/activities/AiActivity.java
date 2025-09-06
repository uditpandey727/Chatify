package com.udit.chatify.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.udit.chatify.R;
import com.udit.chatify.databinding.ActivityAiBinding;
import com.udit.chatify.other.MyFirebaseManager;

public class AiActivity extends AppCompatActivity {
    ActivityAiBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        MyFirebaseManager.setBottomNav(binding.bottomNavigationView,R.id.selectedMsg,R.drawable.ai);
        binding.bottomNavigationView.setOnItemSelectedListener ( item -> {
            int id = item.getItemId();
            if(id == R.id.home){
                item.setChecked(true);
//                startActivity(new Intent(AiActivity.this, HomeActivity.class));
                overridePendingTransition(0, 0); // Disable transition animation
            } else if(id == R.id.users) {
                Toast.makeText(this, "Coming soon...", Toast.LENGTH_SHORT).show();
            }else if(id == R.id.chats){
                item.setChecked(true);
                Intent intent = new Intent(AiActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0); // Disable transition animation
            }else if (id==R.id.profile){
                item.setChecked(true);
//                startActivity(new Intent(AiActivity.this, SettingActivity.class));
                overridePendingTransition(0, 0);
            }
            return true;
        });


    }
}