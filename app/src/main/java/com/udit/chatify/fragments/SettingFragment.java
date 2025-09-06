package com.udit.chatify.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.udit.chatify.Models.User;
import com.udit.chatify.R;
import com.udit.chatify.activities.PhoneNumberActivity;
import com.udit.chatify.databinding.FragmentSettingBinding;

import java.util.Date;
import java.util.HashMap;

public class SettingFragment extends Fragment {

    private FragmentSettingBinding binding;
    private FirebaseDatabase database;
    private FirebaseAuth auth;
    private Uri selectedImage;
    private ProgressDialog dialog;
    private String uid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingBinding.inflate(inflater, container, false);

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        dialog = new ProgressDialog(requireContext());
        dialog.setMessage("Updating Profile");
        dialog.setCancelable(false);



        // Fetch user details
        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            binding.name.setText(user.getName());
                            Glide.with(requireContext()).load(user.getProfileImage())
                                    .placeholder(R.drawable.avatar).into(binding.profileImage);
                            binding.bioText.setText(user.getBio());
                            binding.phoneNumber.setText(user.getPhoneNumber());
                            binding.hidePhone.setChecked(user.getHidePhone());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Upload image
        binding.uploadImgBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imageActivity.launch(intent);
        });

        // Save bio
        binding.saveBtn.setOnClickListener(v -> {
            String bio = binding.bioText.getText().toString();
            uid = FirebaseAuth.getInstance().getUid();
            updateUserData(bio, uid);
        });

        // Hide phone toggle
        binding.hidePhone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            uid = FirebaseAuth.getInstance().getUid();
            DatabaseReference db = database.getReference().child("users").child(uid);
            db.child("hidePhone").setValue(isChecked)
                    .addOnSuccessListener(unused -> Toast.makeText(requireContext(), "Data Updated", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed: " + e, Toast.LENGTH_SHORT).show());
        });

        // Logout button
        binding.logOut.setOnClickListener(v -> showLogoutConfirmationDialog());



        return binding.getRoot();
    }

    private void updateUserData(String bio, String uid) {
        dialog.show();
        if (bio.equals("")) bio = "Hey! Let's Chat";
        DatabaseReference db = database.getReference().child("users").child(uid);
        db.child("bio").setValue(bio).addOnSuccessListener(unused -> dialog.dismiss());
    }

    ActivityResultLauncher<Intent> imageActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri uri = data.getData();
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        long time = new Date().getTime();
                        StorageReference reference = storage.getReference().child("Profiles").child(time + "");
                        reference.putFile(uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()) {
                                    reference.getDownloadUrl().addOnSuccessListener(uri1 -> {
                                        String filePath = uri1.toString();
                                        HashMap<String, Object> obj = new HashMap<>();
                                        obj.put("profileImage", filePath);
                                        database.getReference().child("users")
                                                .child(FirebaseAuth.getInstance().getUid())
                                                .updateChildren(obj);
                                    });
                                }
                            }
                        });
                        selectedImage = uri;
                    }
                }
            });

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to log out?");
        builder.setPositiveButton("Logout", (dialog, which) -> logoutUser());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void logoutUser() {
        auth.signOut();
        Intent intent = new Intent(requireContext(), PhoneNumberActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
