package com.udit.chatify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleSharedVideo();
        finish(); // Finish the activity after handling the shared video
    }

    private void handleSharedVideo() {
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            if ("text/plain".equals(intent.getType())) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    Intent intent1 = new Intent(this, RecipientSelectionActivity.class);
                    intent1.putExtra("shared_text", sharedText);
                    startActivity(intent1);
                    return;
                }
            }
        }
        // If no valid YouTube video URL is found, show an error message or perform an alternative action
        Toast.makeText(this, "Unable to share", Toast.LENGTH_SHORT).show();
    }
}
