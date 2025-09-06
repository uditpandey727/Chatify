package com.udit.chatify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.udit.chatify.Adapters.UsersAdapter;
import com.udit.chatify.Models.User;
import com.udit.chatify.databinding.ActivityRecipientSelectionBinding;

import java.util.ArrayList;

public class RecipientSelectionActivity extends AppCompatActivity {
    ActivityRecipientSelectionBinding binding;

    private ArrayList<User> users;
    private UsersAdapter usersAdapter;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecipientSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Initialization
        database = FirebaseDatabase.getInstance();
        users = new ArrayList<>();

        // Initialize the ListView and the UserListAdapter
        String msg = getIntent().getStringExtra("shared_text");
        usersAdapter = new UsersAdapter(this, users,msg,null);

        // Setting user Adapter
        binding.userListView.setAdapter(usersAdapter);

        // Updating Users Data in Recycler View
        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);
                    if (!(user != null && user.getUid().equals(FirebaseAuth.getInstance().getUid())))
                        //Adding users except current users
                        users.add(user);
                }
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Set click listener for the ListView items
//        binding.userListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                // Retrieve the selected user
//                User selectedUser = users.get(position);
//
//                // Create an Intent to pass the selected user's video URL to the ChatActivity
//                Intent intent = new Intent(RecipientSelectionActivity.this, ChatActivity.class);
//                intent.putExtra("video_url", getVideoUrl());
//                intent.putExtra("recipient_id", selectedUser.getId());
//                intent.putExtra("recipient_name", selectedUser.getName());
//
//                // Start the ChatActivity
//                startActivity(intent);
//            }
//        });
    }
}
