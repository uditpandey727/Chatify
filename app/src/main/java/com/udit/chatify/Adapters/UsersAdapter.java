package com.udit.chatify.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.udit.chatify.Models.Message;
import com.udit.chatify.R;
import com.udit.chatify.Models.User;
import com.udit.chatify.activities.ChatActivity;
import com.udit.chatify.databinding.RowConversationBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UsersViewHolder>{

    Context context;
    ArrayList<User> users;
    ArrayList<User> selectedUsers;
    private UserAdapterListener listener;
    String msg;

    public UsersAdapter(Context context, ArrayList<User> users, String msg, UserAdapterListener listener) {
        this.context = context;
        this.users = users;
        this.msg = msg;
        this.listener = listener;
        selectedUsers = new ArrayList<>();
    }

    public void updateSelectedUsers(ArrayList<User> selectedUsers, boolean isUpdateData) {
        this.selectedUsers = selectedUsers;
        if(isUpdateData){
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_conversation, parent, false);
        return new UsersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
        User user = users.get(position);

        String senderId = FirebaseAuth.getInstance().getUid();
        String senderRoom = senderId + user.getUid();

        // Setting msgTime and Last Message
        FirebaseDatabase.getInstance().getReference().child("chats").child(senderRoom)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String lastMsg = snapshot.child("lastMsg").getValue(String.class);
                            long time = snapshot.child("lastMsgTime").getValue(Long.class);
                            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
                            holder.binding.msgTime.setText(dateFormat.format(new Date(time)));
                            holder.binding.lastMessage.setText(lastMsg);
                        }else{
                            holder.binding.lastMessage.setText("Tap to Chat");
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        holder.binding.userName.setText(user.getName());

        Glide.with(context).load(user.getProfileImage())
                .placeholder(R.drawable.avatar)
                .into(holder.binding.profileImg);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("name", user.getName());
            intent.putExtra("profile", user.getProfileImage());
            intent.putExtra("token", user.getToken());
            intent.putExtra("uid", user.getUid());
            intent.putExtra("bio", user.getBio());
            intent.putExtra("msg", msg);
            if(!user.getHidePhone()) {
                intent.putExtra("phoneNumber", user.getPhoneNumber());
            }else{
                intent.putExtra("phoneNumber", "isPublic");
            }
            context.startActivity(intent);
        });

        int selectedColor = ContextCompat.getColor(context, R.color.selectedColor);
        // Check if the message is selected
        boolean isSelected = selectedUsers.contains(user);
        // Update the background color based on the selection state
        if (isSelected) {
            holder.itemView.setBackgroundColor(selectedColor);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
        holder.itemView.setOnLongClickListener(v -> {
            // Handle the long-press event
            boolean isMessageSelected = false;
            if (listener != null) {
                isMessageSelected = listener.onUserSelected(user);
            }
            if(isMessageSelected){
                holder.itemView.setBackgroundColor(selectedColor);
            }else{
                holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            }
            return true;
        });

    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class UsersViewHolder extends RecyclerView.ViewHolder{
        RowConversationBinding binding;
        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = RowConversationBinding.bind(itemView);
        }
    }

    public interface UserAdapterListener {
        boolean onUserSelected(User user);
    }
}
