package com.udit.chatify.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.udit.chatify.Adapters.GroupMessagesAdapter;
import com.udit.chatify.Models.Message;
import com.udit.chatify.R;
import com.udit.chatify.databinding.ActivityGroupChatBinding;
import com.udit.chatify.databinding.DeleteDialogBinding;
import com.udit.chatify.other.MediaFunctions;
import com.udit.chatify.other.MessageFunctions;
import com.udit.chatify.other.MyFirebaseManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class GroupChatActivity extends AppCompatActivity implements GroupMessagesAdapter.GroupMessageAdapterListener{
    ActivityGroupChatBinding binding;
    GroupMessagesAdapter adapter;
    ArrayList<Message> messages;
    ArrayList<Message> selectedMessages;

    FirebaseDatabase database;
    FirebaseStorage storage;
    DatabaseReference dbRef;

    ProgressDialog dialog;
    String senderUid;
    Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        MyFirebaseManager.firebaseRemoteConfig(GroupChatActivity.this,binding.bg, true);
        initialization();
        adapter = new GroupMessagesAdapter(this,messages, this);
        binding.msgList.setLayoutManager(new LinearLayoutManager(this));
        binding.msgList.setAdapter(adapter);
        loadMessage();
        initializeKeyboardListener();
        MessageFunctions.scrollToLastMessage(binding.msgList,adapter);
        setListener();
    }

    private void initialization(){
        //Setting Top Toolbar
        setSupportActionBar(binding.toolbar);

        // Initialization of values
        senderUid = FirebaseAuth.getInstance().getUid();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        dbRef = database.getReference().child("public").child("messages");

        // sending image Dialog
        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading Image....");
        dialog.setCancelable(false);

        messages = new ArrayList<>();
        selectedMessages = new ArrayList<>();
    }
    private void setListener(){
        binding.sendButton.setOnClickListener(v -> {
            String messageTxt = binding.messageBox.getText().toString();
            if(Objects.equals(messageTxt, "")){
                return;
            }
            sendMessage(false,false,"");
        });
        binding.attachment.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            launchMediaSelectionActivity.launch(intent);
        });
        binding.profileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(GroupChatActivity.this, ProfileView.class);
            intent.putExtra("name", "Group Discussion");
            intent.putExtra("profile", "https://firebasestorage.googleapis.com/v0/b/chatify-fef57.appspot.com/o/Profiles%2Fdiscussion.png?alt=media&token=8c04311a-85a2-4533-bb17-5769fab7b23c");
            intent.putExtra("bio", "This is a discussion group you can share your thoughts, ideas directly to all users of this app");
            intent.putExtra("phoneNumber", "isPublic");
            startActivity(intent);
        });
        binding.backImg.setOnClickListener(v -> finish());
        binding.camera.setOnClickListener(v -> {
            // Create a file to store the captured image
            File photoFile = createImageFile();

            if (photoFile != null) {
                // Get the file URI
                photoUri = FileProvider.getUriForFile(this, "com.udit.chatify.fileprovider", photoFile);
                // Launch the camera app
                captureImageLauncher.launch(photoUri);
            } else {
                // Failed to create the file
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
            }
        });
        binding.scrollToLastButton.setOnClickListener(v -> {
            // Scroll to the last item in the adapter
            binding.msgList.smoothScrollToPosition(adapter.getItemCount() - 1);
        });

        binding.deleteBtn.setOnClickListener(v -> deleteSelectedMessage(selectedMessages));
        binding.copyBtn.setOnClickListener(v -> {
            String copiedText = selectedMessages.get(0).getMessage();

            // Copy the message text to the clipboard
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("message", copiedText);
            clipboard.setPrimaryClip(clip);

            // Show a toast message indicating that the message has been copied
            Toast.makeText(this, "Message copied", Toast.LENGTH_SHORT).show();
            selectedMessages.clear();
            updateHeader(selectedMessages);
            adapter.updateSelectedMessages(selectedMessages,true);
        });
        binding.forwardBtn.setOnClickListener(v -> {
            String forwardText = selectedMessages.get(0).getMessage();
            Intent intent1 = new Intent(GroupChatActivity.this,RecipientSelectionActivity.class);
            intent1.putExtra("shared_text", forwardText);
            startActivity(intent1);
        });

        // Inside onCreate() method in ChatActivity
        binding.msgList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                MessageFunctions.scrollListener(recyclerView,adapter,binding.scrollToLastButton);
            }
        });
    }

    private void loadMessage(){
        database.getReference().child("public").child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        String currentDate = null;
                        for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                            Message message = snapshot1.getValue(Message.class);
                            if (message != null) {
                                Date date = new Date(message.getTimestamp());
                                String messageDate = MessageFunctions.getMessageDate(date);
                                if (!messageDate.equals(currentDate)) {
                                    // Add a header item
                                    Message headerMessage = new Message();
                                    headerMessage.setTimestamp(message.getTimestamp());
                                    headerMessage.setMessageDate(messageDate);
                                    headerMessage.setHeader(true);
                                    messages.add(headerMessage);
                                    currentDate = messageDate;
                                }
                                if (!message.getSenderId().equals(senderUid) && !message.isSeen() && message.getMessageId() != null) {
                                    message.setSeen(true);
                                    dbRef.child(message.getMessageId()).setValue(message);
                                }
                                if (message.isNewMessage()) {
                                    // Only scroll to the last message if it's a new message
                                    MessageFunctions.scrollToLastMessage(binding.msgList,adapter);
                                    message.setNewMessage(false);
                                    dbRef.child(message.getMessageId()).setValue(message);
                                }
                            }
                            messages.add(message);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }
    private void initializeKeyboardListener() {
        final View rootView = getWindow().getDecorView().getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int previousHeight = 0;

            @Override
            public void onGlobalLayout() {
//                if(binding.scrollToLastButton.getVisibility() == View.VISIBLE){return;}
                Rect rect = new Rect();
                rootView.getWindowVisibleDisplayFrame(rect);
                int screenHeight = rootView.getRootView().getHeight();
                int keyboardHeight = screenHeight - rect.bottom;

                if (keyboardHeight > screenHeight * 0.15) {
                    // Keyboard is open
                    MessageFunctions.scrollToLastMessage(binding.msgList,adapter);
                } else if (previousHeight > keyboardHeight) {
                    // Keyboard is closed
                    MessageFunctions.scrollToLastMessage(binding.msgList,adapter);
                }
                previousHeight = keyboardHeight;
            }
        });
    }
    /*public ArrayList<String> getAllUserTokens() {
        ArrayList<String> userTokens = new ArrayList<>();
//        FirebaseDatabase.getInstance().getReference().child("users")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
//                            User user = userSnapshot.getValue(User.class);
//                            if (user != null) {
//                                String token = user.getToken();
//                                boolean receiveNotifications = user.getGroupNotification();
//
//                                if (token != null && receiveNotifications) {
//                                    userTokens.add(token);
//                                }
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//                        // Handle the error
//                    }
//                });
        FirebaseDatabase.getInstance().getReference().child("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot snapshot1 : snapshot.getChildren()){
                    User user = snapshot1.getValue(User.class);
                    Toast.makeText(GroupChatActivity.this, user.getName(), Toast.LENGTH_SHORT).show();
                    if(!user.getUid().equals(FirebaseAuth.getInstance().getUid())){
                        //Adding users except current users
                        String token = user.getToken();
                        userTokens.add(token);}
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
        userTokens.add("cD_UcFBaRECRRqZJSQafHk:APA91bE1UPTpFmIMU-mZNp7YdK8AE-c-4HC0fhf3fim-TYGm2gBjJGVmp04RU5lOal0din5p4yen2As0NEz5ssKAAjrrL4L_ttAA_Dw9fH96W8O3dA4l-H20W0A-YpvFqrjwy-F9XI8t");
        userTokens.add("cXbzW42mQ7q15oLB-WFn04:APA91bET0tJWxnRQJviut2uRoMoTgydxDpDi4lWiQpnto0VT72HjRO9eS-o-IHH-urDr3m-zk2AUkFraiVWefavxzwjoEc9zt-VysreYivhxdyHrUlRRrQpxf3-IPTz-u0f5hQOLMZqo");
        Toast.makeText(this, userTokens.get(1), Toast.LENGTH_SHORT).show();
        return userTokens;
    }

    void sendNotification(String name, String message, ArrayList<String> userTokens) {
        try {
            String url = "https://fcm.googleapis.com/fcm/send";
            // Prepare the FCM notification payload
            JSONObject data = new JSONObject();
            try {
                data.put("title", name);
                data.put("body", message);
            }catch (JSONException e){
                e.printStackTrace();
            }

            JSONObject notificationData = new JSONObject();
            try {
                notificationData.put("notification", data);
                notificationData.put("registration_ids",new JSONArray(userTokens));
            }catch (JSONException e){
                e.printStackTrace();
            }
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, notificationData,
                    response -> {
                        // Handle the successful response
                    },
                    error -> {
//                Toast.makeText(GroupChatActivity.this, error.toString(), Toast.LENGTH_SHORT).show()
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    // Set the Authorization header with your FCM server key
                    Map<String, String> headers = new HashMap<>();
                    String key = "Key=AAAA7u5bq9M:APA91bGPzgML9saT8H41F8-S0tnUfMPT5N0QmNLzSxOImbWxkks9Tw5fK7U8LJXUCtEQYDpcPVh-I0FJF6AZwBc2KhUBX_pOqay8KWjb_fvfJTyhmQO-Be0SlWoqjaF7Wp7pj2QGN_4G";
                    headers.put("Authorization", key);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
        } catch (Exception ex) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show();
        }
    }*/

    // Uploading Image & Video
    ActivityResultLauncher<Intent> launchMediaSelectionActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        Uri selectedMedia = data.getData();
                        String mediaType = getMediaTypeFromUri(selectedMedia);
                        if (mediaType != null) {
                            if (mediaType.equals("image")) {
                                // Selected media is an image
                                MediaFunctions.handleImageSelection(selectedMedia, dialog, this, null);
                            } else if (mediaType.equals("video")) {
                                // Selected media is a video
                                MediaFunctions.handleVideoSelection(selectedMedia, dialog, this, null);
                            }
                        }
                    } else {
                        // Media type is not supported
                        Toast.makeText(this, "Unsupported media type", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    // Define the activity result launcher for capturing an image
    ActivityResultLauncher<Uri> captureImageLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), result -> {
                if (result) {
                    // Image capture successful, handle the captured image
                    MediaFunctions.handleCapturedImage(photoUri, dialog,this,null);
                } else {
                    // Image capture failed or was canceled
                    Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show();
                }
            }
    );
    private String getMediaTypeFromUri(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        String type = contentResolver.getType(uri);
        if (type != null) {
            if (type.startsWith("image/")) {
                return "image";
            } else if (type.startsWith("video/")) {
                return "video";
            }
        }
        return null;
    }
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendMessage(Boolean isPhoto,Boolean isVideo,  String filePath) {
        String messageTxt = binding.messageBox.getText().toString();
        messageTxt = messageTxt.trim();

        Date date = new Date();
        Message message = new Message(messageTxt, senderUid, date.getTime(), true);
        binding.messageBox.setText("");

        if (isPhoto) {
            message.setMessage("photo");
            message.setImageUrl(filePath);
        }else if(isVideo){
            message.setMessage("video");
            message.setVideoThumbnailUrl(this,filePath);
            message.setImageUrl(filePath);
        }
        String randomKey = database.getReference().push().getKey();
        message.setMessageId(randomKey);

        HashMap<String, Object> lastMsgObj = new HashMap<>();
        lastMsgObj.put("lastMsg", message.getMessage());
        lastMsgObj.put("lastMsgTime", date.getTime());

        database.getReference().child("public").updateChildren(lastMsgObj);

        dbRef.child(Objects.requireNonNull(randomKey)).setValue(message).addOnSuccessListener(unused -> {
            message.setSent(true);
            dbRef.child(message.getMessageId()).setValue(message);
        });
    }

    // Functions For deleting Message & updating Header for message Selection
    private void updateHeader(ArrayList<Message> selectedMessagesArr) {
        if (selectedMessagesArr.isEmpty()) {
            // No messages selected, show the default header UI
            binding.profile.setVisibility(View.VISIBLE);
            binding.profileName.setVisibility(View.VISIBLE);
            binding.selectedMsg.setVisibility(View.GONE);
            binding.deleteBtn.setVisibility(View.GONE);
            binding.copyBtn.setVisibility(View.GONE);
            binding.forwardBtn.setVisibility(View.GONE);
        } else if(selectedMessagesArr.size() ==1 ){
            binding.selectedMsg.setText("1");
            // Messages selected, show the delete/copy options in the header UI
            binding.profileName.setVisibility(View.GONE);
            binding.profile.setVisibility(View.GONE);
            binding.selectedMsg.setVisibility(View.VISIBLE);
            binding.deleteBtn.setVisibility(View.VISIBLE);
            binding.copyBtn.setVisibility(View.VISIBLE);
            binding.forwardBtn.setVisibility(View.VISIBLE);
        }else {
            String selectedItems = String.valueOf(selectedMessages.size());
            binding.selectedMsg.setText(selectedItems);
            // Messages selected, show the delete/copy options in the header UI
            binding.profileName.setVisibility(View.GONE);
            binding.profile.setVisibility(View.GONE);
            binding.selectedMsg.setVisibility(View.VISIBLE);
            binding.deleteBtn.setVisibility(View.VISIBLE);
            binding.copyBtn.setVisibility(View.GONE);
            binding.forwardBtn.setVisibility(View.GONE);
        }
    }
    private void deleteSelectedMessage(ArrayList<Message> selectedMessages){
        View view = LayoutInflater.from(this).inflate(R.layout.delete_dialog, null);
        DeleteDialogBinding binding = DeleteDialogBinding.bind(view);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setView(binding.getRoot())
                .create();
        binding.everyone.setVisibility(View.VISIBLE);
        binding.everyone.setOnClickListener(v15 -> {
            deleteMessage(selectedMessages);
            dialog.dismiss();
        });
        binding.delete.setVisibility(View.GONE);

        binding.cancel.setOnClickListener(v14 -> dialog.dismiss());
        dialog.show();
    }
    private void deleteMessage(ArrayList<Message> selectedMessagesArr) {
        for (Message message : selectedMessagesArr) {
            String messageId = message.getMessageId();
            FirebaseDatabase.getInstance().getReference()
                    .child("public")
                    .child("messages")
                    .child(messageId)
                    .setValue(null);
        }
        selectedMessages.clear();
        updateHeader(selectedMessages);
        adapter.updateSelectedMessages(selectedMessages,true);
    }

    @Override
    public void onBackPressed() {
        if(selectedMessages.size()!=0){
            selectedMessages.clear();
            updateHeader(selectedMessages);
            adapter.updateSelectedMessages(selectedMessages,true);
        }else {
            Intent intent = new Intent(GroupChatActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onMessageSelected(Message message) {
        if (selectedMessages != null && selectedMessages.contains(message)) {
            selectedMessages.remove(message);
            adapter.updateSelectedMessages(selectedMessages,false);
            updateHeader(selectedMessages);
            return false;
        } else {
            if (selectedMessages != null) {
                selectedMessages.add(message);
                adapter.updateSelectedMessages(selectedMessages,false);
                updateHeader(selectedMessages);
            }
            return true;
        }
    }

    @Override
    public void setEmoji() {
        selectedMessages.clear();
        adapter.updateSelectedMessages(selectedMessages,false);
        updateHeader(selectedMessages);
    }
}