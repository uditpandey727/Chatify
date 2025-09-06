package com.udit.chatify.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.udit.chatify.Adapters.MessagesAdapter;
import com.udit.chatify.Models.Message;
import com.udit.chatify.databinding.ActivityChatGptactivityBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatGPTActivity extends AppCompatActivity {
    private ActivityChatGptactivityBinding binding;
    ArrayList<Message> messageList;
    MessagesAdapter messageAdapter;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();
    String uid;
    private final String apiKey = "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";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatGptactivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        messageList = new ArrayList<>();
        uid = FirebaseAuth.getInstance().getUid();

        //setup recycler view
        MessagesAdapter.MessageAdapterListener listener = null;
        messageAdapter = new MessagesAdapter(this,messageList, uid, "GPT", listener);
        binding.msgList.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        binding.msgList.setLayoutManager(llm);

        binding.sendButton.setOnClickListener((v)->{
            String question = binding.messageBox.getText().toString().trim();
            addToChat(question,uid);
            binding.messageBox.setText("");
            callAPI(question);
        });
    }

    void addToChat(String message,String uid){
        runOnUiThread(() -> {
            messageList.add(new Message(message,uid,System.currentTimeMillis(),true));
            messageAdapter.notifyDataSetChanged();
            binding.msgList.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    void addResponse(String response){
        messageList.remove(messageList.size()-1);
        addToChat(response,"GPT");
    }

    void callAPI(String question){
        //okhttp
        messageList.add(new Message("Typing... ","GPT", System.currentTimeMillis(),true));

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo");
            JSONArray msgArray = new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("role","user");
            obj.put("content",question);
            msgArray.put(obj);
            jsonBody.put("messages",msgArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(jsonBody.toString(),JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization","Bearer "+ apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to "+e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    JSONObject  jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }else{
//                    addResponse("Failed to load response due to "+response.body().toString());
                    addResponse("Failed to load response due to "+response.body().string());
                }
            }
        });
    }
}