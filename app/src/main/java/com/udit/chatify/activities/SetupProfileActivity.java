package com.udit.chatify.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.udit.chatify.Models.User;
import com.udit.chatify.databinding.ActivitySetupProfileBinding;

import java.util.Date;
import java.util.HashMap;

public class SetupProfileActivity extends AppCompatActivity {

    ActivitySetupProfileBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri selectedImage;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating Profile");
        dialog.setCancelable(false);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        getSupportActionBar().hide();

        binding.profileImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imageActivity.launch(intent);
        });

        binding.continueBtn.setOnClickListener(v -> {
            String name = binding.nameBox.getText().toString();
            if(name.isEmpty()){
                binding.nameBox.setError("Please Type your Name");
                return;
            }
            dialog.show();
            String uid = auth.getUid();
            String phone = auth.getCurrentUser().getPhoneNumber();
            String bio = binding.bioBox.getText().toString();
            Boolean hidePhone = false;
            if(bio.equals("")){bio = "Hey! Let's Chat";}
            if(selectedImage!= null){
                StorageReference reference = storage.getReference().child("Profiles").child(auth.getUid());
                String finalBio = bio;
                reference.putFile(selectedImage).addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        reference.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            User user = new User(uid, name,hidePhone, phone, imageUrl, finalBio);
                            database.getReference()
                                    .child("users")
                                    .child(uid)
                                    .setValue(user)
                                    .addOnSuccessListener(unused -> {
                                        saveData(true, name);
                                        dialog.dismiss();
                                        Intent intent = new Intent (SetupProfileActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    });
                        });
                    }
                });
            }else{
                User user = new User(uid,name, hidePhone, phone, "No Image", bio);

                database.getReference()
                        .child("users")
                        .child(uid)
                        .setValue(user)
                        .addOnSuccessListener(unused -> {
                            dialog.dismiss();
                            saveData(true, name);
                            Intent intent = new Intent (SetupProfileActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        });
            }
        });
    }

    ActivityResultLauncher<Intent> imageActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data.getData() != null & data.getData() != null) {
                        Uri uri = data.getData(); // filepath
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        long time = new Date().getTime();
                        StorageReference reference = storage.getReference().child("Profiles").child(time+"");
                        reference.putFile(uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if(task.isSuccessful()) {
                                    reference.getDownloadUrl().addOnSuccessListener(uri1 -> {
                                        String filePath = uri1.toString();
                                        HashMap<String, Object> obj = new HashMap<>();
                                        obj.put("profileImage", filePath);
                                        database.getReference().child("users")
                                                .child(FirebaseAuth.getInstance().getUid())
                                                .updateChildren(obj).addOnSuccessListener(aVoid -> {});
                                    });
                                }
                            }
                        });

                        binding.profileImage.setImageURI(data.getData());
                        selectedImage = data.getData();
                    }
                }
            });
    public void saveData(Boolean registered, String name) {
        SharedPreferences sharedPref = getSharedPreferences("application", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("isRegistered", registered);
        editor.putString("name", name);
        editor.apply();
    }
}