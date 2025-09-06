package com.udit.chatify.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.udit.chatify.Adapters.MessagesAdapter;
import com.udit.chatify.Models.Message;
import com.udit.chatify.R;
import com.udit.chatify.databinding.ActivityChatBinding;
import com.udit.chatify.databinding.DeleteDialogBinding;
import com.udit.chatify.other.MediaFunctions;
import com.udit.chatify.other.MessageFunctions;
import com.udit.chatify.other.MyFirebaseManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import androidx.activity.OnBackPressedCallback;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity implements MessagesAdapter.MessageAdapterListener{
    ActivityChatBinding binding;
    MessagesAdapter adapter;
    ArrayList<Message> messages;
    ArrayList<Message> selectedMessages;

    FirebaseDatabase database;
    FirebaseStorage storage;
    DatabaseReference senderRoomRef, receiverRoomRef, presenceRef;

    String name, profile, bio,msg,phoneNumber;

    String senderRoom, receiverRoom;
    String receiverUid, senderUid;
    String token, senderName;

    ProgressDialog dialog;

    Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MyFirebaseManager.firebaseRemoteConfig(this,binding.bg, true);
        initialization();
        setUserData(name,profile, msg);

        // Setting MessagesAdapter
        adapter = new MessagesAdapter(this, messages, senderRoom, receiverRoom, this);
        binding.msgList.setLayoutManager(new LinearLayoutManager(this));
        binding.msgList.setAdapter(adapter);

        loadMessage();
        setClickListener(name,profile,bio,phoneNumber);
        initializeKeyboardListener();
        MessageFunctions.scrollToLastMessage(binding.msgList,adapter);
        setScrollAndTextListener();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!selectedMessages.isEmpty()) {
                    selectedMessages.clear();
                    updateHeader(selectedMessages);
                    adapter.updateSelectedMessages(selectedMessages, true);
                } else {
                    Intent intent = new Intent(ChatActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
    private void initialization(){
        // Initializing values
        SharedPreferences sharedPref = getSharedPreferences("application", Context.MODE_PRIVATE);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading Media....");
        dialog.setCancelable(true);

        messages = new ArrayList<>();
        selectedMessages = new ArrayList<>();

        //Setting Top Toolbar
        setSupportActionBar(binding.toolbar);

        // Getting Intent from UsersAdapter
        name = getIntent().getStringExtra("name");
        profile = getIntent().getStringExtra("profile");
        String uid = getIntent().getStringExtra("uid");
        bio = getIntent().getStringExtra("bio");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        msg = getIntent().getStringExtra("msg");
        token = getIntent().getStringExtra("token");
        senderName = sharedPref.getString("name", "Chatify!");

        // Setting User uid & There ChatRooms
        receiverUid = uid;
        senderUid = FirebaseAuth.getInstance().getUid();

        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        presenceRef = database.getReference().child("presence");
        senderRoomRef = database.getReference().child("chats").child(senderRoom).child("messages");
        receiverRoomRef = database.getReference().child("chats").child(receiverRoom).child("messages");
    }

    private void setUserData(String name, String profile,String msg){
        binding.messageBox.setText(msg);
        // Setting User name & Image
        binding.name.setText(name);
        Glide.with(ChatActivity.this).load(profile)
                .placeholder(R.drawable.avatar).into(binding.profile);

        // Setting Online/Offline Status of user
        presenceRef.child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.getValue(String.class);
                    if (status != null && !status.isEmpty()) {
                        if (status.equals("Offline")) {
                            // Displaying Last Seen
                            database.getReference().child("users").child(receiverUid).child("lastSeen")
                                    .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                long lastSeenTimestamp = snapshot.getValue(Long.class);
                                                String lastSeen = MessageFunctions.getLastSeenString(lastSeenTimestamp);
                                                binding.userStatus.setText(lastSeen);
                                                if(lastSeen.equals("Online")){
                                                    binding.userStatus.setTextColor(Color.GREEN);
                                                }else {
                                                    binding.userStatus.setTextColor(Color.parseColor("#868d96"));
                                                }
                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                        } else {
                            binding.userStatus.setTextColor(Color.GREEN);
                            binding.userStatus.setText(status);
                            binding.userStatus.setVisibility(View.VISIBLE);
                        }
                    } else {
                        binding.userStatus.setVisibility(View.GONE);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadMessage(){
        senderRoomRef.addValueEventListener(new ValueEventListener() {
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
                        if (message.getSenderId().equals(receiverUid) && !message.isSeen() && message.getMessageId() != null) {
                            message.setSeen(true);
                            receiverRoomRef.child(message.getMessageId()).setValue(message);
                        }
                        if (message.isNewMessage()) {
                            // Only scroll to the last message if it's a new message
                            MessageFunctions.scrollToLastMessage(binding.msgList,adapter);
                            message.setNewMessage(false);
                            receiverRoomRef.child(message.getMessageId()).setValue(message);
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

    private void setClickListener(String name,String profile,String bio,String phoneNumber){
        binding.sendButton.setOnClickListener(v -> {
            String messageTxt = binding.messageBox.getText().toString();
            if (Objects.equals(messageTxt, "")) {
                return;
            }
            sendMessage(false,false, "");
        });
        // Running Intent for sending a image
        binding.attachment.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            launchMediaSelectionActivity.launch(intent);
        });
        binding.camera.setOnClickListener(v -> {
            // Create a file to store the captured image
            File photoFile = createImageFile();

            if (photoFile != null) {
                // Get the file URI
                photoUri = FileProvider.getUriForFile(this, "com.udit.chatify.fileprovider", photoFile);
                // Launch the camera app
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    // Request the camera Permission if not Granted
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
                }else {
                    captureImageLauncher.launch(photoUri);
                }
            } else {
                // Failed to create the file
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
            }
        });

        // Setting Header's Buttons
        binding.backImg.setOnClickListener(v -> finish());
        binding.profileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, ProfileView.class);
            intent.putExtra("name", name);
            intent.putExtra("profile", profile);
            intent.putExtra("bio", bio);
            intent.putExtra("phoneNumber", phoneNumber);
            startActivity(intent);
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
            Intent intent1 = new Intent(ChatActivity.this,RecipientSelectionActivity.class);
            intent1.putExtra("shared_text", forwardText);
            startActivity(intent1);
        });

        binding.call.setOnClickListener(v -> makePhoneCall(phoneNumber));
        binding.scrollToLastButton.setOnClickListener(v -> {
            // Scroll to the last item in the adapter
            binding.msgList.smoothScrollToPosition(adapter.getItemCount() - 1);
        });
    }

    private void setScrollAndTextListener(){
        // Inside onCreate() method in ChatActivity
        binding.msgList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                MessageFunctions.scrollListener(recyclerView,adapter,binding.scrollToLastButton);
            }
        });
        // Checking Typing.... Status
        final Handler handler = new Handler();
        binding.messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                presenceRef.child(senderUid).setValue("Typing....");
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStoppedTyping, 1000);
            }

            final Runnable userStoppedTyping = new Runnable() {
                @Override
                public void run() {
                    presenceRef.child(senderUid).setValue("Online");
                }
            };
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

    private void makePhoneCall(String phoneNumber) {
        if(phoneNumber.equals("isPublic")){
            return;
        }
        Uri uri = Uri.parse("tel:" + phoneNumber);
        Intent intent = new Intent(Intent.ACTION_CALL, uri);
        startActivity(intent);
    }

    // Function for Sending Messages & Notification
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

        database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
        database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

        senderRoomRef.child(Objects.requireNonNull(randomKey)).setValue(message).addOnSuccessListener(unused -> {
            message.setSent(true);
            senderRoomRef.child(message.getMessageId()).setValue(message);
            receiverRoomRef.child(randomKey).setValue(message).addOnSuccessListener(unused1 -> presenceRef.child(receiverUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String presence = null;
                    if(snapshot.exists()){
                        presence = snapshot.getValue(String.class);
                    }
                    if(presence == null || presence.equals("Offline")){
                        fetchAccessTokenAndSend(senderName, message.getMessage(), token);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            }));
        });
    }

    private void fetchAccessTokenAndSend(String name, String message, String targetToken) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Replace with your deployed Cloudflare Worker URL
                String workerUrl = "https://mute-bar-f0ce.uditpandey727.workers.dev/";

                Request request = new Request.Builder()
                        .url(workerUrl)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : null;

                if (response.isSuccessful() && responseBody != null) {
                    JSONObject json = new JSONObject(responseBody);
                    String accessToken = json.getString("access_token");

                    // Now call sendNotification with the token
                    sendNotification(name, message, targetToken, accessToken);
                } else {
                    Log.e("FCM", "Failed to fetch access token: " + responseBody);
                }

            } catch (Exception e) {
                Log.e("FCM", "Error fetching token", e);
            }
        }).start();
    }

    private void sendNotification(String name, String message, String targetToken, String accessToken) {
        if (targetToken == null || targetToken.isEmpty()) {
            runOnUiThread(() ->
                    Toast.makeText(ChatActivity.this, "Target token is empty", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Notification visible in system tray
                JSONObject notification = new JSONObject();
                notification.put("title", name);
                notification.put("body", message);

                // Extra data (for click actions in app)
                JSONObject dataObj = new JSONObject();
                dataObj.put("sender", name);
                dataObj.put("text", message);

                // Message body
                JSONObject messageObj = new JSONObject();
                messageObj.put("token", targetToken);
                messageObj.put("notification", notification);
                messageObj.put("data", dataObj);

                JSONObject root = new JSONObject();
                root.put("message", messageObj);

                RequestBody body = RequestBody.create(
                        root.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url("https://fcm.googleapis.com/v1/projects/YOUR_PROJECT_ID/messages:send")
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "null";

                Log.d("FCM", "Response Code: " + response.code());
                Log.d("FCM", "Response Body: " + responseBody);

            } catch (Exception e) {
                Log.e("FCM", "Error sending FCM", e);
            }
        }).start();
    }

    // Functions For deleting Message & updating Header for message Selection
    private void updateHeader(ArrayList<Message> selectedMessagesArr) {
        if (selectedMessagesArr.isEmpty()) {
            // No messages selected, show the default header UI
            binding.call.setVisibility(View.VISIBLE);
            binding.videoCall.setVisibility(View.VISIBLE);
            binding.userData.setVisibility(View.VISIBLE);
            binding.profile.setVisibility(View.VISIBLE);
            binding.selectedMsg.setVisibility(View.GONE);
            binding.deleteBtn.setVisibility(View.GONE);
            binding.copyBtn.setVisibility(View.GONE);
            binding.forwardBtn.setVisibility(View.GONE);
        } else if(selectedMessagesArr.size() ==1 ){
            binding.selectedMsg.setText("1");
            // Messages selected, show the delete/copy options in the header UI
            binding.call.setVisibility(View.GONE);
            binding.videoCall.setVisibility(View.GONE);
            binding.userData.setVisibility(View.GONE);
            binding.profile.setVisibility(View.GONE);
            binding.selectedMsg.setVisibility(View.VISIBLE);
            binding.deleteBtn.setVisibility(View.VISIBLE);
            binding.copyBtn.setVisibility(View.VISIBLE);
            binding.forwardBtn.setVisibility(View.VISIBLE);
        }else {
            String selectedItems = String.valueOf(selectedMessages.size());
            binding.selectedMsg.setText(selectedItems);
            // Messages selected, show the delete/copy options in the header UI
            binding.call.setVisibility(View.GONE);
            binding.videoCall.setVisibility(View.GONE);
            binding.userData.setVisibility(View.GONE);
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
            deleteMessage(selectedMessages, true);
            dialog.dismiss();
        });

        binding.delete.setOnClickListener(v16 -> {
            deleteMessage(selectedMessages, false);
            dialog.dismiss();
        });

        binding.cancel.setOnClickListener(v14 -> dialog.dismiss());
        dialog.show();
    }
    private void deleteMessage(ArrayList<Message> selectedMessagesArr, Boolean isEveryone) {
        for (Message message : selectedMessagesArr) {
            String messageId = message.getMessageId();
            FirebaseDatabase.getInstance().getReference()
                    .child("chats")
                    .child(senderRoom)
                    .child("messages")
                    .child(messageId)
                    .setValue(null);
            if (isEveryone) {
                FirebaseDatabase.getInstance().getReference()
                        .child("chats")
                        .child(receiverRoom)
                        .child("messages")
                        .child(messageId)
                        .setValue(null);
            }
        }
        selectedMessages.clear();
        updateHeader(selectedMessages);
        adapter.updateSelectedMessages(selectedMessages,true);
    }

    // Handle the result of the permission request

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0) {
                captureImageLauncher.launch(photoUri);
            } else {
                Toast.makeText(this, "Give Camera Permission to take Pictures", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

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
                                MediaFunctions.handleImageSelection(selectedMedia, dialog,null, this);
                            } else if (mediaType.equals("video")) {
                                // Selected media is a video
                                MediaFunctions.handleVideoSelection(selectedMedia, dialog, null, this);
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
                    MediaFunctions.handleCapturedImage(photoUri, dialog,null,this);
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
            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
//            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return null;
    }


    // Override Methods
    @Override
    protected void onResume() {
        super.onResume();
        if (senderUid != null) {
            presenceRef.child(senderUid).setValue("Online");

            DatabaseReference lastSeenRef = database.getReference("users").child(senderUid).child("lastSeen");
            lastSeenRef.setValue(ServerValue.TIMESTAMP);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (senderUid != null) {
            presenceRef.child(senderUid).setValue("Offline");
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