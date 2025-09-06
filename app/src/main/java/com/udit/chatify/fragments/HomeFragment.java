package com.udit.chatify.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.udit.chatify.R;
import com.udit.chatify.databinding.DialogFormBinding;
import com.udit.chatify.databinding.FragmentHomeBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private DatabaseReference databaseReference;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentHomeBinding.bind(view);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("studyHub");

        // Get user details from SharedPreferences
        SharedPreferences sharedPref = requireContext().getSharedPreferences("application", Context.MODE_PRIVATE);
        String senderName = sharedPref.getString("name","Chatify!");
        String senderProfile = sharedPref.getString("profile","");

        // Load avatar
        Glide.with(requireContext())
                .load(senderProfile)
                .placeholder(R.drawable.avatar)
                .into(binding.avatar);

        binding.avatar.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.udit.chatify.activities.ImageViewerActivity.class);
            intent.putExtra("image_url", senderProfile);
            intent.putExtra("image_title", senderName);
            startActivity(intent);
        });

        // Study Corner - Update Study Hours Button
        Button updateHoursButton = binding.updateHoursButton;
        updateHoursButton.setOnClickListener(v -> showDialog());

        binding.inshightBtn.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.udit.chatify.activities.InsightActivity.class))
        );
    }

    private void showDialog() {
        final Dialog dialog = new Dialog(requireContext());
        DialogFormBinding dialogBinding = DialogFormBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        dialog.setCancelable(true);

        String uid = FirebaseAuth.getInstance().getUid();

        dialogBinding.submitButton.setOnClickListener(v -> {
            float phyHr = Float.parseFloat(dialogBinding.phyHr.getText().toString());
            float chemHr = Float.parseFloat(dialogBinding.chemHr.getText().toString());
            float mathHr = Float.parseFloat(dialogBinding.mathHr.getText().toString());
            float booksHr = Float.parseFloat(dialogBinding.classHr.getText().toString());

            String currentDate = new SimpleDateFormat("E, d MMM", Locale.ENGLISH).format(new Date());

            DatabaseReference studyHoursRef = FirebaseDatabase.getInstance().getReference()
                    .child("studyHub")
                    .child(uid)
                    .child(currentDate);

            studyHoursRef.child("phyHr").setValue(phyHr);
            studyHoursRef.child("chemHr").setValue(chemHr);
            studyHoursRef.child("mathHr").setValue(mathHr);
            studyHoursRef.child("classHr").setValue(booksHr);

            Toast.makeText(requireContext(), "Study hours updated successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // prevent memory leaks
    }
}
