package com.udit.chatify.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.udit.chatify.Adapters.TopStatusAdapter;
import com.udit.chatify.Adapters.UsersAdapter;
import com.udit.chatify.Models.Status;
import com.udit.chatify.Models.User;
import com.udit.chatify.Models.UserStatus;
import com.udit.chatify.R;
import com.udit.chatify.activities.ChatGPTActivity;
import com.udit.chatify.activities.GroupChatActivity;
import com.udit.chatify.databinding.DeleteDialogBinding;
import com.udit.chatify.databinding.FragmentMainBinding;
import com.udit.chatify.other.MediaFunctions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;

public class MainFragment extends Fragment implements UsersAdapter.UserAdapterListener{
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private FragmentMainBinding binding;

    FirebaseDatabase database;
    ArrayList<User> users;
    ArrayList<User> selectedUsers;
    UsersAdapter usersAdapter;
    TopStatusAdapter statusAdapter;
    ArrayList<UserStatus> userStatuses;
    ProgressDialog dialog;

    User user;


    public MainFragment() {
    }

    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
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
        binding = FragmentMainBinding.inflate(inflater);
        if(getActivity()!= null){
            initialization();
            onClickListeners();
            firebaseManager();
        }

        return binding.getRoot();
    }
    private void initialization(){
        // Initialization
        database = FirebaseDatabase.getInstance();



        //Dialog for Sending image
        dialog = new ProgressDialog(getActivity());
        dialog.setMessage("Uploading Image.....");
        dialog.setCancelable(false);

        users = new ArrayList<>();
        userStatuses = new ArrayList<>();

        usersAdapter = new UsersAdapter(getActivity(), users,"",this);
        statusAdapter = new TopStatusAdapter(getActivity(), userStatuses);

        selectedUsers = new ArrayList<>();

        // Setting user & Status Adapter
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.statusList.setLayoutManager(layoutManager);
        binding.statusList.setAdapter(statusAdapter);
        binding.userListView.setAdapter(usersAdapter);

        // For Loading Shimmer Effect
        binding.userListView.showShimmerAdapter();
        binding.statusList.showShimmerAdapter();
    }

    private void onClickListeners(){
        // Adding Status
        binding.addStatus.setOnClickListener(v -> {
            //Uploading Status...
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            imageActivity.launch(intent);
        });

        // Opening Group Chat Activity
        binding.publicChat.setOnClickListener(v ->startActivity(new Intent(getActivity(), GroupChatActivity.class)));
        binding.ChatGPT.setOnClickListener(v ->startActivity(new Intent(getActivity(), ChatGPTActivity.class)));
        binding.deleteBtn.setOnClickListener(v -> deleteSelectedUser(selectedUsers));
    }

    private void firebaseManager(){
        final Activity activity = getActivity(); // Store a reference to the Activity
        if (activity == null) {
            return;
        }
        // Getting the Current user To use in uploading Status
//        database.getReference().child("users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
//                .addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        user = snapshot.getValue(User.class);
//                        SharedPreferences sharedPref = requireActivity().getSharedPreferences("application", Context.MODE_PRIVATE);
//                        SharedPreferences.Editor editor = sharedPref.edit();
//                        editor.putString("profile",user.getProfileImage());
//                        editor.putString("name", user.getName());
//                        editor.apply();
//                        Glide.with(requireActivity())
//                                .load(user.getProfileImage())
//                                .placeholder(R.drawable.avatar)
//                                .into(binding.image);
//                    }
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {}
//                });

        // Setting msgTime and Last Message

        database.getReference().child("public")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String lastMsg = snapshot.child("lastMsg").getValue(String.class);
                            long time = snapshot.child("lastMsgTime").getValue(Long.class);
                            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
                            binding.msgTime.setText(dateFormat.format(new Date(time)));
                            binding.lastMessage.setText(lastMsg);
                        }else{
                            binding.lastMessage.setText("Tap to Chat");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        // Updating Users Data in Recycler View (only rooms you own)
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid == null) return;

        DatabaseReference chatRef = database.getReference().child("chats");
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                HashSet<String> addedUserIds = new HashSet<>();

                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    String roomId = roomSnapshot.getKey();
                    if (roomId == null) continue;

                    // ✅ Only consider rooms that YOU own: "<currentUid><otherUid>"
                    if (!roomId.startsWith(currentUid)) continue;

                    String otherUserId = roomId.substring(currentUid.length());
                    if (otherUserId.isEmpty() || addedUserIds.contains(otherUserId)) continue;
                    addedUserIds.add(otherUserId);

                    database.getReference().child("users").child(otherUserId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    User u = userSnap.getValue(User.class);
                                    if (u != null) {
                                        users.add(u);
                                        binding.userListView.hideShimmerAdapter();
                                        usersAdapter.notifyDataSetChanged();
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });



        // Updating Users Data in Recycler View
//        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                users.clear();
//                for(DataSnapshot snapshot1 : snapshot.getChildren()){
//                    User user = snapshot1.getValue(User.class);
//                    if(!(user != null && user.getUid().equals(FirebaseAuth.getInstance().getUid())))
//                        //Adding users except current users
//                        users.add(user);
//                }
//                binding.userListView.hideShimmerAdapter();
//                usersAdapter.notifyDataSetChanged();
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {}
//        });

        // Updating Status to the Status List
        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    userStatuses.clear();
                    for(DataSnapshot storySnapshot: snapshot.getChildren()){
                        // This For loop is run for each users
                        UserStatus status = new UserStatus();
                        status.setName(storySnapshot.child("name").getValue(String.class));
                        status.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));
                        status.setLastUpdated(storySnapshot.child("lastUpdated").getValue(Long.class));

                        ArrayList<Status> statuses = new ArrayList<>();

                        for(DataSnapshot statusSnapshot: storySnapshot.child("statuses").getChildren()){
                            // This loop will load all single user Stories
                            Status sampleStatus = statusSnapshot.getValue(Status.class);
                            statuses.add(sampleStatus);
                        }
                        status.setStatuses(statuses);
                        userStatuses.add(status);
                    }
                    binding.statusList.hideShimmerAdapter();
                    statusAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    // Functions For deleting User & updating Header for message Selection
    private void updateHeader(ArrayList<User> selectedUsersArr) {
        if (selectedUsersArr.isEmpty()) {
            // No messages selected, show the default header UI
            binding.appName.setVisibility(View.VISIBLE);
            binding.mainToolbar.setVisibility(View.GONE);
        }
        else {
            String selectedItems = String.valueOf(selectedUsers.size());
            binding.selectedUser.setText(selectedItems);
            // Users selected, show the delete/copy options in the header UI
            binding.appName.setVisibility(View.GONE);
            binding.mainToolbar.setVisibility(View.VISIBLE);
        }
    }
    private void deleteSelectedUser(ArrayList<User> selectedUsers){
        View view = LayoutInflater.from(getContext()).inflate(R.layout.delete_dialog, null);
        DeleteDialogBinding binding = DeleteDialogBinding.bind(view);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Delete User")
                .setView(binding.getRoot())
                .create();
        binding.everyone.setVisibility(View.VISIBLE);
        binding.everyone.setOnClickListener(v15 -> {
            deleteUser(selectedUsers, true);
            dialog.dismiss();
        });

        binding.delete.setOnClickListener(v16 -> {
            deleteUser(selectedUsers, false);
            dialog.dismiss();
        });

        binding.cancel.setOnClickListener(v14 -> dialog.dismiss());
        dialog.show();
    }
    private void deleteUser(ArrayList<User> selectedUsersArr, Boolean isEveryone) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        for (User user : selectedUsersArr) {
            String selectedUserId = user.getUid();
            String senderRoom = currentUid + selectedUserId;

            DatabaseReference chatsRef = database.getReference().child("chats");
            chatsRef.child(senderRoom)
                    .removeValue()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(), "Chat deleted for you", Toast.LENGTH_SHORT).show();
                        // ✅ remove from list immediately
                        usersAdapter.notifyDataSetChanged();
                    });
            if (isEveryone) {
                String receiverRoom = selectedUserId + currentUid;
                chatsRef.child(receiverRoom).removeValue()
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(getContext(), "Chat deleted for both", Toast.LENGTH_SHORT).show();
                            // ✅ also remove locally
                            usersAdapter.notifyDataSetChanged();
                        });
            }
        }
        selectedUsers.clear();
        updateHeader(selectedUsers);
        usersAdapter.updateSelectedUsers(selectedUsers,true);
    }

    @Override
    public boolean onUserSelected(User user) {
        if (selectedUsers != null && selectedUsers.contains(user)) {
            selectedUsers.remove(user);
            usersAdapter.updateSelectedUsers(selectedUsers,false);
            updateHeader(selectedUsers);
            return false;
        } else {
            if (selectedUsers != null) {
                selectedUsers.add(user);
                usersAdapter.updateSelectedUsers(selectedUsers,false);
                updateHeader(selectedUsers);
            }
            return true;
        }
    }

    ActivityResultLauncher<Intent> imageActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (Objects.requireNonNull(data).getData() != null & data.getData() != null) {
                        dialog.show();
                        MediaFunctions.setStatus(data,user,dialog);
                    }
                }
            });
}