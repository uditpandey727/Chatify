package com.udit.chatify.other;


import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.udit.chatify.Models.Status;
import com.udit.chatify.Models.User;
import com.udit.chatify.Models.UserStatus;
import com.udit.chatify.activities.ChatActivity;
import com.udit.chatify.activities.GroupChatActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class MediaFunctions {
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();

    public static void setStatus(Intent data, User user, Dialog dialog) {
        Date date = new Date();
        StorageReference reference = storage.getReference().child("status").child(date.getTime() + "");
        reference.putFile(data.getData()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                    UserStatus userStatus = new UserStatus();
                    userStatus.setName(user.getName());
                    userStatus.setProfileImage(user.getProfileImage());
                    userStatus.setLastUpdated(date.getTime());

                    HashMap<String, Object> obj = new HashMap<>();
                    obj.put("name", userStatus.getName());
                    obj.put("profileImage", userStatus.getProfileImage());
                    obj.put("lastUpdated", userStatus.getLastUpdated());

                    String imageUrl = uri.toString();
                    Status status = new Status(imageUrl, userStatus.getLastUpdated());

                    database.getReference()
                            .child("stories")
                            .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                            .updateChildren(obj);

                    database.getReference().child("stories")
                            .child(FirebaseAuth.getInstance().getUid())
                            .child("statuses")
                            .push()
                            .setValue(status);
                    dialog.dismiss();
                });
            }
        });
    }

    public static void handleImageSelection(Uri selectedImage, Dialog dialog, GroupChatActivity groupChatActivity, ChatActivity chatActivity) {
        // Handle image selection logic
        Calendar calendar = Calendar.getInstance();
        StorageReference reference = storage.getReference().child("chats").child(calendar.getTimeInMillis() + "");
        dialog.show();
        reference.putFile(selectedImage).addOnCompleteListener(task -> {
            dialog.dismiss();
            if (task.isSuccessful()) {
                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String filePath = uri.toString();
                    if(chatActivity==null){
                        groupChatActivity.sendMessage(true, false, filePath);
                    }else {
                        chatActivity.sendMessage(true, false, filePath);
                    }
                });
            } else {
                Toast.makeText(dialog.getContext(), "Upload Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public static void handleVideoSelection(Uri selectedVideo, Dialog dialog, GroupChatActivity groupChatActivity, ChatActivity chatActivity) {
        Calendar calendar = Calendar.getInstance();
        StorageReference reference = storage.getReference().child("chats").child(calendar.getTimeInMillis() + ".mp4");
        dialog.show();
        // Upload the selected video to storage and send it as a message
        reference.putFile(selectedVideo).addOnCompleteListener(task -> {
            dialog.dismiss();
            if (task.isSuccessful()) {
                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String filePath = uri.toString();
                    if(chatActivity== null){
                        groupChatActivity.sendMessage(false, true, filePath);
                    }else {
                        chatActivity.sendMessage(false, true, filePath);
                    }
                });
            } else {
                Toast.makeText(dialog.getContext(), "Upload Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public static void handleCapturedImage(Uri photoUri, Dialog dialog, GroupChatActivity groupChatActivity, ChatActivity chatActivity) {
        // Process the captured image, such as uploading it or displaying it in the chat interface
        if (photoUri != null) {
            handleImageSelection(photoUri, dialog,groupChatActivity,chatActivity);
        } else {
            Toast.makeText(dialog.getContext(), "Failed to get the captured image URI", Toast.LENGTH_SHORT).show();
        }
    }
}
