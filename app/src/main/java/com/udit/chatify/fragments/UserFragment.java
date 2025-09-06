package com.udit.chatify.fragments;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.udit.chatify.Adapters.UsersAdapter;
import com.udit.chatify.Models.User;
import com.udit.chatify.databinding.FragmentUserBinding;

import java.util.ArrayList;

public class UserFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private FragmentUserBinding binding;

    FirebaseDatabase database;
    ArrayList<User> users;
    UsersAdapter usersAdapter;

    public UserFragment() {}


    public static UserFragment newInstance(String param1, String param2) {
        UserFragment fragment = new UserFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentUserBinding.inflate(inflater);
        if(getActivity()!= null){
            initialization();
            firebaseManager();
        }

        return binding.getRoot();
    }
    private void initialization(){
        // Initialization
        database = FirebaseDatabase.getInstance();

        users = new ArrayList<>();

        usersAdapter = new UsersAdapter(getActivity(), users,"",null);

        // Setting user & Status Adapter
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.userListView.setAdapter(usersAdapter);

        // For Loading Shimmer Effect
        binding.userListView.showShimmerAdapter();
    }

    private void firebaseManager(){
        final Activity activity = getActivity(); // Store a reference to the Activity
        if (activity == null) {
            return;
        }

        // Updating Users Data in Recycler View
        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for(DataSnapshot snapshot1 : snapshot.getChildren()){
                    User user = snapshot1.getValue(User.class);
                    if(!(user != null && user.getUid().equals(FirebaseAuth.getInstance().getUid())))
                        //Adding users except current users
                        users.add(user);
                }
                binding.userListView.hideShimmerAdapter();
                usersAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}